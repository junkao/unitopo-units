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

package io.frinx.unitopo.unit.xr7.bgp.handler.neighbor

import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.handlers.bgp.BgpListWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.As
import io.frinx.unitopo.unit.xr7.bgp.UnderlayNeighbor
import io.frinx.unitopo.unit.xr7.bgp.UnderlayNeighborBuilder
import io.frinx.unitopo.unit.xr7.bgp.UnderlayNeighborKey
import io.frinx.unitopo.unit.xr7.bgp.UnderlayVrfNeighborBuilder
import io.frinx.unitopo.unit.xr7.bgp.UnderlayVrfNeighborKey
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalConfigWriter
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalConfigWriter.Companion.XR_BGP_INSTANCE_NAME
import io.frinx.unitopo.unit.xr7.bgp.handler.toUnderlay
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.InstanceAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.InstanceAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.FourByteAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.BgpEntity
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.Neighbors
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.neighbor.content.NeighborAfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.neighbor.content.neighbor.afs.NeighborAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfNeighbors
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighbor
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.vrf.neighbor.content.VrfNeighborAfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.vrf.neighbor.content.vrf.neighbor.afs.VrfNeighborAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.password.PasswordBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.remote.`as`.RemoteAsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev170626.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.ProprietaryPassword
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.BgpCommonNeighborGroupTransportConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.Bgp
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.NeighborKey as NativeNeighborKey

class NeighborWriter(private val access: UnderlayAccess) : BgpListWriter<Neighbor, NeighborKey> {

    override fun writeCurrentAttributesForType(
        instanceIdentifier: InstanceIdentifier<Neighbor>,
        neighbor: Neighbor,
        writeContext: WriteContext
    ) {
        val vrfKey = instanceIdentifier.firstKeyOf(NetworkInstance::class.java)

        val bgpGlobal = writeContext.readAfter(RWUtils.cutId(instanceIdentifier, Bgp::class.java))
            .get().global
        val bgpAs = bgpGlobal?.config?.`as`!!

        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
            val neighborBuilder = UnderlayNeighborBuilder()
            renderGlobalNeighbor(neighborBuilder, neighbor)
            access.put(getGlobalNeighborIdentifier(bgpAs, neighbor.neighborAddress.toNoZone()),
                neighborBuilder.build())
        } else {
            val neighborBuilder = UnderlayVrfNeighborBuilder()
            renderVrfNeighbor(neighborBuilder, neighbor)
            access.put(getVrfNeighborIdentifier(bgpAs, vrfKey, neighbor.neighborAddress.toNoZone()),
                neighborBuilder.build())
        }
    }

    override fun updateCurrentAttributesForType(
        instanceIdentifier: InstanceIdentifier<Neighbor>,
        dataBefore: Neighbor,
        dataAfter: Neighbor,
        writeContext: WriteContext
    ) {
        val vrfKey = instanceIdentifier.firstKeyOf(NetworkInstance::class.java)

        val bgpGlobal = writeContext.readAfter(RWUtils.cutId(instanceIdentifier, Bgp::class.java)).get().global
        val bgpAs = bgpGlobal?.config?.`as`!!

        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
            val id = getGlobalNeighborIdentifier(bgpAs, dataAfter.neighborAddress.toNoZone())
            val builder = access.read(id)
                .checkedGet()
                .or(UnderlayNeighborBuilder().build())
                .let {
                    UnderlayNeighborBuilder(it)
                }
            renderGlobalNeighbor(builder, dataAfter)
            access.put(id, builder.build())
        } else {
            val id = getVrfNeighborIdentifier(bgpAs, vrfKey, dataAfter.neighborAddress.toNoZone())
            val builder = access.read(id)
                .checkedGet()
                .or(UnderlayVrfNeighborBuilder().build())
                .let {
                    UnderlayVrfNeighborBuilder(it)
                }
            renderVrfNeighbor(builder, dataAfter)
            access.put(id, builder.build())
        }
    }

    override fun deleteCurrentAttributesForType(
        instanceIdentifier: InstanceIdentifier<Neighbor>,
        neighbor: Neighbor,
        writeContext: WriteContext
    ) {
        val vrfKey = instanceIdentifier.firstKeyOf(NetworkInstance::class.java)

        val bgpGlobal = writeContext.readBefore(RWUtils.cutId(instanceIdentifier, Bgp::class.java)).get().global
        val bgpAs = bgpGlobal?.config?.`as`!!

        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
            access.delete(getGlobalNeighborIdentifier(bgpAs, neighbor.neighborAddress.toNoZone()))
        } else {
            access.delete(getVrfNeighborIdentifier(bgpAs, vrfKey, neighbor.neighborAddress.toNoZone()))
        }
    }

    companion object {

        fun getVrfNeighborIdentifier(bgpProcess: AsNumber, vrfName: NetworkInstanceKey, neighbor: IpAddressNoZone):
            InstanceIdentifier<VrfNeighbor> {
            val (asXX, asYY) = As.asToDotNotation(bgpProcess)

            return GlobalConfigWriter.XR_BGP_ID
                    .child(Instance::class.java, InstanceKey(XR_BGP_INSTANCE_NAME))
                    .child(InstanceAs::class.java, InstanceAsKey(BgpAsRange(asXX)))
                    .child(FourByteAs::class.java, FourByteAsKey(BgpAsRange(asYY)))
                    .child(Vrfs::class.java)
                    .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfName.name)))
                    .child(VrfNeighbors::class.java)
                    .child(VrfNeighbor::class.java, UnderlayVrfNeighborKey(neighbor))
        }

        fun renderGlobalNeighbor(
            builder: UnderlayNeighborBuilder,
            data: Neighbor
        ) {
            data.config.peerAs?.let {
                val (asXX, asYY) = As.asToDotNotation(it)
                builder.setNeighborAddress(data.neighborAddress.toNoZone())
                        .setUpdateSourceInterface(data.transport?.config?.localAddress?.toIfcName()).remoteAs =
                        RemoteAsBuilder()
                                .setAsXx(BgpAsRange(asXX))
                                .setAsYy(BgpAsRange(asYY))
                                .build()
            }

            if (data.config.authPassword == null) {
                builder.password = null
            } else {
                builder.password = PasswordBuilder().apply {
                    password = ProprietaryPassword("!" + data.config.authPassword.value)
                    isPasswordDisable = false
                }.build()
            }

            builder.description = data.config.description

            val underlayNeighborAfiList = data.afiSafis?.afiSafi?.map {
                NeighborAfBuilder().apply {
                    this.afName = it.afiSafiName.toUnderlay()
                    this.setActivate(true)
                }.build()
            }.orEmpty().toMutableList()

            if (underlayNeighborAfiList.isEmpty()) {
                builder.neighborAfs = null
            }
            builder.neighborAfs = NeighborAfsBuilder()
                    .setNeighborAf(underlayNeighborAfiList)
                    .build()
            builder.setKey(NativeNeighborKey(data.neighborAddress.toNoZone()))
        }

        fun getGlobalNeighborIdentifier(bgpProcess: AsNumber, neighbor: IpAddressNoZone):
            InstanceIdentifier<UnderlayNeighbor> {
            val (asXX, asYY) = As.asToDotNotation(bgpProcess)

            return GlobalConfigWriter.XR_BGP_ID
                    .child(Instance::class.java, InstanceKey(XR_BGP_INSTANCE_NAME))
                    .child(InstanceAs::class.java, InstanceAsKey(BgpAsRange(asXX)))
                    .child(FourByteAs::class.java, FourByteAsKey(BgpAsRange(asYY)))
                    .child(DefaultVrf::class.java)
                    .child(BgpEntity::class.java)
                    .child(Neighbors::class.java)
                    .child(UnderlayNeighbor::class.java, UnderlayNeighborKey(neighbor))
        }

        fun renderVrfNeighbor(
            builder: UnderlayVrfNeighborBuilder,
            data: Neighbor
        ) {
            val (asXX, asYY) = As.asToDotNotation(data.config.peerAs)

            builder.setNeighborAddress(data.neighborAddress.toNoZone())
                    .setUpdateSourceInterface(data.transport?.config?.localAddress?.toIfcName()).remoteAs =
                    RemoteAsBuilder()
                            .setAsXx(BgpAsRange(asXX))
                            .setAsYy(BgpAsRange(asYY))
                            .build()

            if (data.config.authPassword == null) {
                builder.password = null
            } else {
                builder.password = PasswordBuilder().apply {
                    password = ProprietaryPassword("!" + data.config.authPassword.value)
                    isPasswordDisable = false
                }.build()
            }

            builder.description = data.config.description

            val underlayNeighborAfiList = data.afiSafis?.afiSafi?.map {
                VrfNeighborAfBuilder().apply {
                    this.afName = it.afiSafiName.toUnderlay()
                    this.setActivate(true)
                }.build()
            }.orEmpty().toMutableList()
            if (underlayNeighborAfiList.isEmpty()) {
                builder.vrfNeighborAfs = null
            }
            builder.vrfNeighborAfs = VrfNeighborAfsBuilder()
                .setVrfNeighborAf(underlayNeighborAfiList)
                .build()
        }
    }
}

private fun BgpCommonNeighborGroupTransportConfig.LocalAddress?.toIfcName(): InterfaceName? {
    return this?.string?.let {
        InterfaceName(it)
    }
}