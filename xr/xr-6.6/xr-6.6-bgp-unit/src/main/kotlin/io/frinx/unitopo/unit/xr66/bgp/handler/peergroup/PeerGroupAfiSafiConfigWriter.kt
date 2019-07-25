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

package io.frinx.unitopo.unit.xr66.bgp.handler.peergroup

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.As
import io.frinx.unitopo.unit.xr66.bgp.handler.BgpProtocolReader
import io.frinx.unitopo.unit.xr66.bgp.handler.toUnderlay
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.InstanceAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.InstanceAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.FourByteAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.BgpEntity
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.NeighborGroups
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.NeighborGroup
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.NeighborGroupKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.neighbor.group.NeighborGroupAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.neighbor.group.neighbor.group.afs.NeighborGroupAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.neighbor.group.neighbor.group.afs.NeighborGroupAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.neighbor.group.neighbor.group.afs.NeighborGroupAfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev170626.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.afi.safi.list.afi.safi.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.list.PeerGroup
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.AFISAFITYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class PeerGroupAfiSafiConfigWriter(private val access: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: IID<Config>,
        config: Config,
        context: WriteContext
    ) {
        val protocolKey = id.firstKeyOf(Protocol::class.java)
        val asNumber = PeerGroupConfigWriter.getAsNumber(id, context::readAfter)
        val peerGroupName = id.firstKeyOf(PeerGroup::class.java).peerGroupName
        val underlayId = getNeighborGroupAfUnderlayId(asNumber, protocolKey, peerGroupName, config.afiSafiName)
        val data = createUnderlayBuilder(underlayId, config).build()
        access.safePut(underlayId, data)
    }

    override fun updateCurrentAttributes(
        id: org.opendaylight.yangtools.yang.binding.InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        context: WriteContext
    ) {
        writeCurrentAttributes(id, dataAfter, context)
    }

    override fun deleteCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        context: WriteContext
    ) {
        val protocolKey = id.firstKeyOf(Protocol::class.java)
        val asNumber = PeerGroupConfigWriter.getAsNumber(id, context::readBefore)
        val peerGroupName = id.firstKeyOf(PeerGroup::class.java).peerGroupName
        val underlayId = getNeighborGroupAfUnderlayId(asNumber, protocolKey, peerGroupName, dataBefore.afiSafiName)
        val data = createUnderlayBuilder(underlayId, dataBefore).build()
        access.safeDelete(underlayId, data)
    }

    private fun createUnderlayBuilder(id: IID<NeighborGroupAf>, config: Config): NeighborGroupAfBuilder {
        val underlayData = access.read(id).checkedGet().orNull()
        val builder = when (underlayData) {
            null -> NeighborGroupAfBuilder()
            else -> NeighborGroupAfBuilder(underlayData)
        }
        builder.setActivate(true)
        builder.setAfName(config.afiSafiName.toUnderlay())
        return builder
    }

    companion object {
        fun getNeighborGroupAfUnderlayId(
            asN: AsNumber,
            key: ProtocolKey,
            neighborGroupName: String,
            ocAfiSafi: Class<out AFISAFITYPE>
        ): IID<NeighborGroupAf> {
            val (aXX, aYY) = As.asToDotNotation(asN)
            return BgpProtocolReader.UNDERLAY_BGP
                .child(Instance::class.java, InstanceKey(CiscoIosXrString(key.name)))
                .child(InstanceAs::class.java, InstanceAsKey(BgpAsRange(aXX)))
                .child(FourByteAs::class.java, FourByteAsKey(BgpAsRange(aYY)))
                .child(DefaultVrf::class.java)
                .child(BgpEntity::class.java)
                .child(NeighborGroups::class.java)
                .child(NeighborGroup::class.java, NeighborGroupKey(CiscoIosXrString(neighborGroupName)))
                .child(NeighborGroupAfs::class.java)
                .child(NeighborGroupAf::class.java, NeighborGroupAfKey(ocAfiSafi.toUnderlay()))
        }
    }
}