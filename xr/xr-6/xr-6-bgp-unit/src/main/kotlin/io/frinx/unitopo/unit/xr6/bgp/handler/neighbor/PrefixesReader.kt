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
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.UnderlayOperNeighbor
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.BgpAfi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.state.Prefixes
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.state.PrefixesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.AFISAFITYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.IPV4UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.IPV6UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.L3VPNIPV4UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.L3VPNIPV6UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class PrefixesReader(private val access: UnderlayAccess) : OperReaderCustomizer<Prefixes, PrefixesBuilder> {

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Prefixes>,
        builder: PrefixesBuilder,
        readContext: ReadContext
    ) {
        val neighborKey = id.firstKeyOf(Neighbor::class.java)
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val afiSafiKey = id.firstKeyOf(AfiSafi::class.java)

        val data = access.read(NeighborStateReader.getId(protKey, vrfKey, neighborKey))
                .checkedGet()
                .orNull()

        builder.fromUnderlay(data, afiSafiKey)
    }
}

fun PrefixesBuilder.fromUnderlay(data: UnderlayOperNeighbor?, afiSafiKey: AfiSafiKey) {
    data?.afData.orEmpty()
            .find { it.afName.toOpenconfig() == afiSafiKey.afiSafiName }
            ?.let {
                received = it.prefixesAccepted
            }
}

fun BgpAfi.toOpenconfig(): Class<out AFISAFITYPE>? {
    when (this) {
        BgpAfi.Ipv4 -> return IPV4UNICAST::class.java
        BgpAfi.VpNv4 -> return L3VPNIPV4UNICAST::class.java
        BgpAfi.VpNv6 -> return L3VPNIPV6UNICAST::class.java
        BgpAfi.Ipv6 -> return IPV6UNICAST::class.java
    }

    return null
}