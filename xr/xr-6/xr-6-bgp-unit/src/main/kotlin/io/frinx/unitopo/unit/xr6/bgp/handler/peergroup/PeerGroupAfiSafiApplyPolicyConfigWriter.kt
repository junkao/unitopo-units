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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.handler.toUnderlay
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.neighbor.group.neighbor.group.afs.NeighborGroupAfBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.list.PeerGroup
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.apply.policy.group.apply.policy.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.apply.policy.group.apply.policy.ConfigBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class PeerGroupAfiSafiApplyPolicyConfigWriter(private val access: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: IID<Config>,
        config: Config,
        context: WriteContext
    ) {
        val protocolKey = id.firstKeyOf(Protocol::class.java)
        val asNumber = PeerGroupConfigWriter.getAsNumber(id, context::readAfter)
        val peerGroupName = id.firstKeyOf(PeerGroup::class.java).peerGroupName
        val afiSafiName = id.firstKeyOf(AfiSafi::class.java).afiSafiName
        val underlayId = PeerGroupAfiSafiConfigWriter.getNeighborGroupAfUnderlayId(asNumber,
            protocolKey, peerGroupName, afiSafiName)
        val underlayData = access.read(underlayId).checkedGet().orNull()
        val data = when (underlayData) {
            null -> NeighborGroupAfBuilder()
            else -> NeighborGroupAfBuilder(underlayData)
        }
            .setActivate(true)
            .setAfName(afiSafiName.toUnderlay())
            .setRoutePolicyIn(config.importPolicy?.first())
            .setRoutePolicyOut(config.exportPolicy?.first())
            .build()
        access.safePut(underlayId, data)
    }

    override fun updateCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        context: WriteContext
    ) {
        safeMerge(id, dataAfter, context)
    }

    override fun deleteCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        context: WriteContext
    ) {
        // using merge instead of delete because underlay is just a leaf
        safeMerge(id, ConfigBuilder().build(), context)
    }

    private fun safeMerge(
        id: IID<Config>,
        dataAfter: Config,
        context: WriteContext
    ) {
        val protocolKey = id.firstKeyOf(Protocol::class.java)
        val asNumber = PeerGroupConfigWriter.getAsNumber(id, context::readAfter)
        val peerGroupName = id.firstKeyOf(PeerGroup::class.java).peerGroupName
        val afiSafiName = id.firstKeyOf(AfiSafi::class.java).afiSafiName
        val underlayId = PeerGroupAfiSafiConfigWriter.getNeighborGroupAfUnderlayId(asNumber,
            protocolKey, peerGroupName, afiSafiName)
        val underlayBefore = access.read(underlayId).checkedGet().orNull()!!
        val data = NeighborGroupAfBuilder(underlayBefore)
            .setRoutePolicyIn(dataAfter.importPolicy?.first())
            .setRoutePolicyOut(dataAfter.exportPolicy?.first())
            .build()
        access.safeMerge(underlayId, underlayBefore, underlayId, data)
    }
}