/*
 * Copyright © 2019 Frinx and others.
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
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.common.structure.neighbor.group.ebgp.multihop.ebgp.multihop.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.common.structure.neighbor.group.ebgp.multihop.ebgp.multihop.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NeighborEbgpMultihopConfigReader(private val access: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Config>,
        builder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val neighborKey = id.firstKeyOf(Neighbor::class.java)
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val data = access.read(BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java,
                InstanceKey(CiscoIosXrString(protKey.name))))
                .checkedGet()
                .orNull()

        parseEbgpMultihop(data, neighborKey, builder)
    }

    companion object {
        fun parseEbgpMultihop(
            underlayInstance: Instance?,
            neighborKey: NeighborKey,
            builder: ConfigBuilder
        ) {
            val first4ByteAs = BgpProtocolReader.getFirst4ByteAs(underlayInstance)
            val ebgpMultihop = NeighborConfigReader.getNeighbor(first4ByteAs, neighborKey)?.ebgpMultihop

            ebgpMultihop?.isMplsDeactivation?.let { builder.setEnabled(it) }
            ebgpMultihop?.maxHopCount?.let { builder.setMultihopTtl(it.toShort()) }
        }
    }
}