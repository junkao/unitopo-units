/*
 * Copyright © 2018 Frinx and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.frinx.unitopo.unit.xr6.bgp.handler.aggregates

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.builder.Check
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalAfiSafiReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfGlobal
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.NiProtAggAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.NiProtAggAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.Aggregate
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class BgpLocalAggregateConfigReader(private val underlayAccess: UnderlayAccess) :
    CompositeReader.Child<Config, ConfigBuilder> {

    override fun getCheck(): Check {
        return ChecksMap.PathCheck.Protocol.BGP
    }

    override fun getBuilder(p0: InstanceIdentifier<Config>) = ConfigBuilder()

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val aggregateKey = id.firstKeyOf(Aggregate::class.java)
        configBuilder.prefix = aggregateKey.prefix
        val key = id.firstKeyOf(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val aggrKey = id.firstKeyOf(Aggregate::class.java)
        val bgpInstance = underlayAccess.read(
            BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString(key.name))))
            .checkedGet()
            .orNull()
        configBuilder.fromUnderlay(bgpInstance, aggrKey, vrfKey.name)
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as AggregateBuilder).config = config
    }

    companion object {
        fun ConfigBuilder.fromUnderlay(
            instance: Instance?,
            aggrKey: AggregateKey,
            vrfName: String
        ) {
            val globals = arrayListOf<VrfGlobal>()
            instance?.instanceAs?.map {
                it.fourByteAs?.map {
                    if (NetworInstance.DEFAULT_NETWORK_NAME == vrfName) {
                        val policyName = GlobalAfiSafiReader.getGlobalAfs(it).firstOrNull()
                            ?.sourcedNetworks?.sourcedNetwork.orEmpty().firstOrNull()?.routePolicyName
                        prefix = aggrKey.prefix
                        val niProtAggAug = NiProtAggAugBuilder().apply {
                            policyName?.let {
                                this.applyPolicy = listOf(it)
                            }
                        }.build()
                        addAugmentation(NiProtAggAug::class.java, niProtAggAug)
                    } else {
                        it.vrfs?.vrf?.find {
                            it.vrfName.value == vrfName
                        }?.let { globals.add(it.vrfGlobal) }
                    }
                }
            }

            globals.map {
                it.vrfGlobalAfs?.let {
                    it.vrfGlobalAf?.map {
                        it.aggregateAddresses?.aggregateAddress?.filter {
                            it.aggregateAddr.value.size > 0
                        }?.filter {
                            aggrKey.prefix.value.joinToString("") ==
                                "${it.aggregateAddr.ipv4Address.value}/${it.aggregatePrefix}"
                        }?.map {
                            prefix = aggrKey.prefix
                            val niProtAggAug = NiProtAggAugBuilder().apply {
                                it.routePolicyName?.let {
                                    this.applyPolicy = listOf(it)
                                }
                            }.build()
                            addAugmentation(NiProtAggAug::class.java, niProtAggAug)
                        }
                    }
                }
            }
        }
    }
}