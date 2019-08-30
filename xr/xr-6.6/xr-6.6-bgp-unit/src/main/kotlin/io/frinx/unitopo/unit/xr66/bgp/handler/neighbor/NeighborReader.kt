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

package io.frinx.unitopo.unit.xr66.bgp.handler.neighbor

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr66.bgp.UnderlayNeighbor
import io.frinx.unitopo.unit.xr66.bgp.handler.BgpProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighbor
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.NeighborsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv4Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv6Address
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class NeighborReader(private val access: UnderlayAccess) :
    ConfigListReaderCustomizer<Neighbor, NeighborKey, NeighborBuilder> {

    override fun getAllIds(id: InstanceIdentifier<Neighbor>, readContext: ReadContext): List<NeighborKey> {
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        val data = access.read(BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java,
            InstanceKey(CiscoIosXrString(protKey.name))))
            .checkedGet()
            .orNull()

        return parseNeighbors(data, vrfKey)
    }

    override fun merge(builder: Builder<out DataObject>, list: List<Neighbor>) {
        (builder as NeighborsBuilder).neighbor = list
    }

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Neighbor>,
        neighborBuilder: NeighborBuilder,
        readContext: ReadContext
    ) {
        neighborBuilder.neighborAddress = id.firstKeyOf(Neighbor::class.java).neighborAddress
    }

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<Neighbor>) = NeighborBuilder()

    companion object {
        fun parseNeighbors(data: Instance?, vrfKey: NetworkInstanceKey): List<NeighborKey> {
            val fourByteAs = BgpProtocolReader.getFirst4ByteAs(data)

            val neighbors = if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
                getNeighbors(fourByteAs)
                    .map { it.neighborAddress }
            } else {
                getVrfNeighbors(fourByteAs, vrfKey)
                    .map { it.neighborAddress }
            }

            return neighbors
                .map { it.toIp() }
                .filterNotNull()
                .map { NeighborKey(it) }
                .toList()
        }

        fun getVrfNeighbors(fourByteAs: FourByteAs?, vrfKey: NetworkInstanceKey): List<VrfNeighbor> {
            return fourByteAs
                ?.vrfs
                ?.vrf.orEmpty()
                .find { it.vrfName.value == vrfKey.name }
                ?.vrfNeighbors
                ?.vrfNeighbor.orEmpty()
        }

        fun getNeighbors(fourByteAs: FourByteAs?): List<UnderlayNeighbor> {
            return fourByteAs
                ?.defaultVrf
                ?.bgpEntity
                ?.neighbors
                ?.neighbor.orEmpty()
        }
    }
}

fun IpAddressNoZone.toIp(): IpAddress? {
    if (ipv4AddressNoZone != null) {
        return IpAddress(Ipv4Address(ipv4AddressNoZone.value))
    } else {
        return IpAddress(Ipv6Address(ipv6AddressNoZone.value))
    }
}