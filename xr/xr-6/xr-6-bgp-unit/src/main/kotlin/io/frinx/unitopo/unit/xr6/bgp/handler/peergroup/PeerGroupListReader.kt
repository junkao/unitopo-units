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

package io.frinx.unitopo.unit.xr6.bgp.handler.peergroup

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.UnderlayNeighborGroup
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.base.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.list.PeerGroup
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.list.PeerGroupBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.list.PeerGroupKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class PeerGroupListReader(private val access: UnderlayAccess) :
    ConfigListReaderCustomizer<PeerGroup, PeerGroupKey, PeerGroupBuilder> {

    override fun getAllIds(id: InstanceIdentifier<PeerGroup>, readContext: ReadContext): List<PeerGroupKey> {
        val vrfKey = id.firstKeyOf<NetworkInstance, NetworkInstanceKey>(NetworkInstance::class.java)
        if (vrfKey.name != NetworInstance.DEFAULT_NETWORK_NAME) {
            return emptyList()
        }
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val data = access.read(BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java,
            InstanceKey(CiscoIosXrString(protKey.name))))
            .checkedGet()
            .orNull()
        return parsePeerGroups(data)
    }

    override fun readCurrentAttributes(
        id: InstanceIdentifier<PeerGroup>,
        builder: PeerGroupBuilder,
        readContext: ReadContext
    ) {
        builder.peerGroupName = id.firstKeyOf(PeerGroup::class.java).peerGroupName
        builder.config = ConfigBuilder().apply {
            peerGroupName = builder.peerGroupName
        }.build()
    }

    companion object {
        fun parsePeerGroups(data: Instance?): List<PeerGroupKey> {
            val fourByteAs = BgpProtocolReader.getFirst4ByteAs(data)

            val peerGroups = getPeerGroups(fourByteAs)

            return peerGroups
                .map { it.neighborGroupName }
                .filterNotNull()
                .map { PeerGroupKey(it.value) }
                .toList()
        }

        fun getPeerGroups(fourByteAs: FourByteAs?): List<UnderlayNeighborGroup> {
            return fourByteAs
                ?.defaultVrf
                ?.bgpEntity
                ?.neighborGroups
                ?.neighborGroup.orEmpty()
        }

        fun getPeerGroup(fourByteAs: FourByteAs?, key: PeerGroupKey) =
                getPeerGroups(fourByteAs).find { it.neighborGroupName.value == key.peerGroupName }
    }
}