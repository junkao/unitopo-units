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

package io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.afisafi

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.common.mp.all.afi.safi.common.PrefixLimitBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.common.mp.all.afi.safi.common.prefix.limit.ConfigBuilder as PrefixLimitConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.common.mp.ipv4.ipv6.unicast.common.ConfigBuilder as Ipv6UnicastConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.common.mp.ipv6.unicast.group.Ipv6Unicast
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.common.mp.ipv6.unicast.group.Ipv6UnicastBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class Ipv6UnicastReader(private val access: UnderlayAccess) :
        ConfigReaderCustomizer<Ipv6Unicast, Ipv6UnicastBuilder> {

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Ipv6Unicast>,
        builder: Ipv6UnicastBuilder,
        readContext: ReadContext
    ) {

        val neighborKey = id.firstKeyOf(Neighbor::class.java)
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val afKey = id.firstKeyOf<AfiSafi, AfiSafiKey>(AfiSafi::class.java)
        val data = access.read(BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java,
                InstanceKey(CiscoIosXrString(protKey.name))))
                .checkedGet()
                .orNull()

        parseAfisafiIpv6Unicast(data, neighborKey, afKey, builder)
    }

    companion object {
        fun parseAfisafiIpv6Unicast(
            underlayInstance: Instance?,
            neighborKey: NeighborKey,
            afKey: AfiSafiKey,
            builder: Ipv6UnicastBuilder
        ) {
            val neighborAf = AfiSafiReader.findNeighborAf(underlayInstance, neighborKey, afKey)
            val maxPrefix = neighborAf?.maximumPrefixes?.prefixLimit
            val sendDefaultRoute = neighborAf?.defaultOriginate?.isEnable

            maxPrefix?.let { builder.setPrefixLimit(PrefixLimitBuilder()
                    .setConfig(PrefixLimitConfigBuilder().setMaxPrefixes(it).build())
                    .build()) }
            sendDefaultRoute?.let { builder.setConfig(Ipv6UnicastConfigBuilder().setSendDefaultRoute(it).build()) }
        }
    }
}