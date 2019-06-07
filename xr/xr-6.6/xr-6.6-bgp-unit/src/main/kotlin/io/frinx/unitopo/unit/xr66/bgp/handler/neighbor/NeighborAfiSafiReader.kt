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
import io.frinx.unitopo.unit.xr66.bgp.handler.BgpProtocolReader
import io.frinx.unitopo.unit.xr66.bgp.handler.toOpenconfig
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.afi.safi.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.AfiSafisBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class NeighborAfiSafiReader(private val access: UnderlayAccess) :
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

    override fun merge(builder: Builder<out DataObject>, list: List<AfiSafi>) {
        (builder as AfiSafisBuilder).afiSafi = list
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

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<AfiSafi>) = AfiSafiBuilder()

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
    }
}