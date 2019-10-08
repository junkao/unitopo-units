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

import com.google.common.base.Optional
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.As
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.BgpEntity
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.NeighborGroups
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.NeighborGroup
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.NeighborGroupBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.NeighborGroupKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class PeerGroupConfigWriter(private val access: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: IID<Config>,
        config: Config,
        context: WriteContext
    ) {
        val protocolKey = id.firstKeyOf(Protocol::class.java)
        requires(context, id)
        val asNumber = getAsNumber(id, context::readAfter)
        val underlayId = getUnderlayId(asNumber, protocolKey, config.peerGroupName)
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
        requires(context, id)
        val asNumber = getAsNumber(id, context::readBefore)
        val underlayId = getUnderlayId(asNumber, protocolKey, dataBefore.peerGroupName)
        val data = createUnderlayBuilder(underlayId, dataBefore).build()
        access.safeDelete(underlayId, data)
    }

    private fun getUnderlayId(
        asN: AsNumber,
        key: ProtocolKey,
        name: String
    ): IID<NeighborGroup> {
        val (aXX, aYY) = As.asToDotNotation(asN)
        return BgpProtocolReader.UNDERLAY_BGP
            .child(Instance::class.java, InstanceKey(CiscoIosXrString(key.name)))
            .child(InstanceAs::class.java, InstanceAsKey(BgpAsRange(aXX)))
            .child(FourByteAs::class.java, FourByteAsKey(BgpAsRange(aYY)))
            .child(DefaultVrf::class.java)
            .child(BgpEntity::class.java)
            .child(NeighborGroups::class.java)
            .child(NeighborGroup::class.java, NeighborGroupKey(CiscoIosXrString(name)))
    }

    private fun createUnderlayBuilder(id: IID<NeighborGroup>, config: Config): NeighborGroupBuilder {
        val underlayData = access.read(id).checkedGet().orNull()
        val builder = when (underlayData) {
            null -> NeighborGroupBuilder()
            else -> NeighborGroupBuilder(underlayData)
        }
        builder.setNeighborGroupName(CiscoIosXrString(config.peerGroupName))
        builder.setCreate(true)
        return builder
    }

    private fun requires(context: WriteContext, id: IID<Config>) {
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        require(vrfKey == NetworInstance.DEFAULT_NETWORK,
            { "Can't add neighbor-group in network-insance ${vrfKey.name}." })
    }

    companion object {
        fun <T : DataObject> getAsNumber(
            id: IID<T>,
            read: (id: IID<Protocol>) -> Optional<Protocol>
        ): AsNumber {
            val protocolId = RWUtils.cutId(id, Protocol::class.java)
            return read(protocolId).get().bgp.global.config.`as`
        }
    }
}