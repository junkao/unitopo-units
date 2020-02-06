/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.bgp.handler.aggregates

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.builder.Check
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.bgp.handler.BgpProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfGlobal
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.Aggregate
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpPrefix
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class BgpAggregateReader(private val access: UnderlayAccess) :
        CompositeListReader.Child<Aggregate, AggregateKey, AggregateBuilder> {

    override fun getCheck(): Check {
        return ChecksMap.PathCheck.Protocol.BGP
    }

    override fun getBuilder(id: IID<Aggregate>) = AggregateBuilder()

    override fun getAllIds(
        id: IID<Aggregate>,
        readContext: ReadContext
    ): List<AggregateKey> {

        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
            return emptyList()
        }

        val protocolKey = id.firstKeyOf(Protocol::class.java)
        val bgpInstance = access.read(
                BgpProtocolReader.UNDERLAY_BGP
                        .child(Instance::class.java, InstanceKey(CiscoIosXrString(protocolKey.name))))
                .checkedGet()
                .orNull()

        return parseAggregates(bgpInstance, vrfKey.name)
    }

    override fun readCurrentAttributes(
        IID: IID<Aggregate>,
        aggregateBuilder: AggregateBuilder,
        readContext: ReadContext
    ) {
        aggregateBuilder.prefix = IID.firstKeyOf(Aggregate::class.java).prefix
    }

    companion object {
        private fun parseAggregates(instance: Instance?, vrfName: String): List<AggregateKey> {
            val globals = arrayListOf<VrfGlobal>()
            val rtn = arrayListOf<AggregateKey>()
            instance?.instanceAs?.map {
                it.fourByteAs?.map {
                    it.vrfs?.vrf?.filter {
                        it.vrfName.value == vrfName
                    }?.map {
                        globals.add(it.vrfGlobal)
                    }
                }
            }

            globals?.map {
                it.vrfGlobalAfs?.let {
                    it.vrfGlobalAf?.map {
                        it.aggregateAddresses?.aggregateAddress?.filter {
                            it.aggregateAddr.value.size > 0
                        }?.map {
                            rtn.add(
                                AggregateKey(
                                    IpPrefix(
                                        "${it.aggregateAddr.ipv4Address.value}/${it.aggregatePrefix}"
                                            .toCharArray()
                                    )
                                )
                            )
                        }
                    }
                }
            }
            return rtn
        }
    }
}