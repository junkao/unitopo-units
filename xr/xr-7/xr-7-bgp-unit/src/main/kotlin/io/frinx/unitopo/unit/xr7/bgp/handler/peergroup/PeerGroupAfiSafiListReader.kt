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

package io.frinx.unitopo.unit.xr7.bgp.handler.peergroup

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.bgp.handler.BgpProtocolReader
import io.frinx.unitopo.unit.xr7.bgp.handler.toOpenconfig
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.afi.safi.list.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.afi.safi.list.afi.safi.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.list.PeerGroup
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.list.PeerGroupKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class PeerGroupAfiSafiListReader(private val access: UnderlayAccess) :
    ConfigListReaderCustomizer<AfiSafi, AfiSafiKey, AfiSafiBuilder> {

    override fun getAllIds(id: InstanceIdentifier<AfiSafi>, readContext: ReadContext): List<AfiSafiKey> {
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val pgKey = id.firstKeyOf(PeerGroup::class.java)
        val data = access.read(BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java,
            InstanceKey(CiscoIosXrString(protKey.name))))
            .checkedGet()
            .orNull()
        return parseAfiSafis(data, pgKey)
    }

    override fun readCurrentAttributes(
        id: InstanceIdentifier<AfiSafi>,
        afiSafiBuilder: AfiSafiBuilder,
        readContext: ReadContext
    ) {
        afiSafiBuilder.afiSafiName = id.firstKeyOf(AfiSafi::class.java).afiSafiName
        afiSafiBuilder.config = ConfigBuilder()
            .setAfiSafiName(afiSafiBuilder.afiSafiName)
            .build()
    }

    companion object {
        fun parseAfiSafis(data: Instance?, pgKey: PeerGroupKey): List<AfiSafiKey> {
            val fourByteAs = BgpProtocolReader.getFirst4ByteAs(data)

            val afs = getPeerGroup(fourByteAs, pgKey)
                    ?.neighborGroupAfs
                    ?.neighborGroupAf.orEmpty()
                    .map { it.afName }
            return afs
                .map { it.toOpenconfig() }
                .filterNotNull()
                .map { AfiSafiKey(it) }
                .toList()
        }

        fun getPeerGroup(fourByteAs: FourByteAs?, key: PeerGroupKey) =
                PeerGroupListReader.getPeerGroups(fourByteAs)
                        .find { it.neighborGroupName.value == key.peerGroupName }
    }
}