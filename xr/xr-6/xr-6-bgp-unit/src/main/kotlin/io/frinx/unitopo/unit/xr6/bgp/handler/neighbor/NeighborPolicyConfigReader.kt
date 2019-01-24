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

package io.frinx.unitopo.unit.xr6.bgp.handler.neighbor

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.handlers.bgp.BgpReader
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.apply.policy.group.ApplyPolicyBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.apply.policy.group.apply.policy.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.apply.policy.group.apply.policy.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NeighborPolicyConfigReader(private val access: UnderlayAccess) :
    BgpReader.BgpConfigReader<Config, ConfigBuilder> {

    override fun merge(parentBuilder: Builder<out DataObject>, config: Config) {
        (parentBuilder as ApplyPolicyBuilder).config = config
    }

    override fun getBuilder(p0: InstanceIdentifier<Config>) = ConfigBuilder()

    override fun readCurrentAttributesForType(
        id: InstanceIdentifier<Config>,
        builder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val neighborKey = id.firstKeyOf(Neighbor::class.java)
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        val data = access.read(BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java,
            InstanceKey(CiscoIosXrString(protKey.name))))
                .checkedGet()
                .orNull()

        parseNeighbor(data, vrfKey, neighborKey, builder)
    }

    companion object {
        fun parseNeighbor(
            underlayInstance: Instance?,
            vrfKey: NetworkInstanceKey,
            neighborKey: NeighborKey,
            builder: ConfigBuilder
        ) {
            val fourByteAs = BgpProtocolReader.getFirst4ByteAs(underlayInstance)
            val afs: List<Pair<String, String>> = if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
                NeighborConfigReader.getNeighbor(fourByteAs, neighborKey)
                        ?.neighborAfs
                        ?.neighborAf.orEmpty()
                        .map { Pair(it.routePolicyIn, it.routePolicyOut) }
            } else {
                NeighborConfigReader.getVrfNeighbor(fourByteAs, vrfKey, neighborKey)
                        ?.vrfNeighborAfs
                        ?.vrfNeighborAf.orEmpty()
                        .map { Pair(it.routePolicyIn, it.routePolicyOut) }
            }

            builder.fromUnderlay(afs)
        }
    }
}

private fun ConfigBuilder.fromUnderlay(policies: List<Pair<String?, String?>>) {
    importPolicy = mutableListOf()
    exportPolicy = mutableListOf()

    policies.forEach { (inP, outP) ->
        inP?.let {
            importPolicy.add(inP)
        }
        outP?.let {
            exportPolicy.add(outP)
        }
    }
}