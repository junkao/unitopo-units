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

package io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.afisafi

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborConfigReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborWriter
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.toNoZone
import io.frinx.unitopo.unit.xr6.bgp.handler.toOpenconfig
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.neighbor.neighbor.afs.NeighborAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.BgpNeAfAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.BgpNeAfAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.soft.reconfiguration.group.SoftReconfigurationBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.afi.safi.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class AfiSafiReader(private val access: UnderlayAccess) :
    ConfigListReaderCustomizer<AfiSafi, AfiSafiKey, AfiSafiBuilder> {

    override fun getAllIds(id: InstanceIdentifier<AfiSafi>, readContext: ReadContext): List<AfiSafiKey> {
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val neighborKey = id.firstKeyOf(Neighbor::class.java)

        val data = access.read(BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java,
            InstanceKey(CiscoIosXrString(protKey.name))))
                .checkedGet()
                .orNull()

        return parseAfiSafis(data, vrfKey, neighborKey)
    }

    override fun readCurrentAttributes(
        id: InstanceIdentifier<AfiSafi>,
        afiSafiBuilder: AfiSafiBuilder,
        readContext: ReadContext
    ) {

        val neighborKey = id.firstKeyOf(Neighbor::class.java)
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val afKey = id.firstKeyOf<AfiSafi, AfiSafiKey>(AfiSafi::class.java)

        val data = access.read(BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java,
                InstanceKey(CiscoIosXrString(protKey.name))))
                .checkedGet()
                .orNull()

        afiSafiBuilder.afiSafiName = id.firstKeyOf(AfiSafi::class.java).afiSafiName

        val configBuilder = ConfigBuilder().setAfiSafiName(afiSafiBuilder.afiSafiName)
        val softReconfiguration = findNeighborAf(data, neighborKey, afKey)?.softReconfiguration
        softReconfiguration?.let { configBuilder.addAugmentation(BgpNeAfAug::class.java, BgpNeAfAugBuilder()
                .setSoftReconfiguration(SoftReconfigurationBuilder()
                        .setAlways(it.isSoftAlways)
                        .build())
                .build()) }

        afiSafiBuilder.config = configBuilder.setAfiSafiName(afiSafiBuilder.afiSafiName).build()
    }

    companion object {
        fun parseAfiSafis(data: Instance?, vrfKey: NetworkInstanceKey, neighborKey: NeighborKey): List<AfiSafiKey> {
            val fourByteAs = BgpProtocolReader.getFirst4ByteAs(data)

            val afs = if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
                NeighborConfigReader.getNeighbor(fourByteAs, neighborKey)
                        ?.neighborAfs
                        ?.neighborAf.orEmpty()
                        .map { it.afName }
            } else {
                NeighborConfigReader.getVrfNeighbor(fourByteAs, vrfKey, neighborKey)
                        ?.vrfNeighborAfs
                        ?.vrfNeighborAf.orEmpty()
                        .map { it.afName }
            }

            return afs
                    .map { it.toOpenconfig() }
                    .filterNotNull()
                    .map { AfiSafiKey(it) }
                    .toList()
        }

        fun findNeighborAf(
            underlayInstance: Instance?,
            neighborKey: NeighborKey,
            afKey: AfiSafiKey
        ): NeighborAf? {
            return BgpProtocolReader.getFirst4ByteAs(underlayInstance)
                    ?.defaultVrf
                    ?.bgpEntity
                    ?.neighbors
                    ?.neighbor
                    ?.find { it.key.neighborAddress == neighborKey.neighborAddress.toNoZone() }
                    ?.neighborAfs
                    ?.neighborAf
                    ?.find { it.key.afName.getName() == NeighborWriter.transformAfiToString(afKey) }
        }
    }
}