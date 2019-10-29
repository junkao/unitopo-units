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

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.As
import io.frinx.unitopo.unit.xr6.bgp.UnderlayNeighbor
import io.frinx.unitopo.unit.xr6.bgp.UnderlayNeighborBuilder
import io.frinx.unitopo.unit.xr6.bgp.UnderlayNeighborKey
import io.frinx.unitopo.unit.xr6.bgp.UnderlayVrfNeighborBuilder
import io.frinx.unitopo.unit.xr6.bgp.UnderlayVrfNeighborKey
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalConfigWriter
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalConfigWriter.Companion.XR_BGP_INSTANCE_NAME
import io.frinx.unitopo.unit.xr6.bgp.handler.toUnderlay
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.BgpEntity
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.Neighbors
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.neighbor.NeighborAfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.neighbor.neighbor.afs.NeighborAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfNeighbors
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighbor
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.vrf.neighbor.VrfNeighborAfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.vrf.neighbor.vrf.neighbor.afs.VrfNeighborAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.remote.`as`.RemoteAsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.BgpCommonNeighborGroupTransportConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.Bgp
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.Global
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.AFISAFITYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NeighborWriter(private val access: UnderlayAccess) : ListWriterCustomizer<Neighbor, NeighborKey> {

    override fun writeCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Neighbor>,
        neighbor: Neighbor,
        writeContext: WriteContext
    ) {
        val vrfKey = instanceIdentifier.firstKeyOf(NetworkInstance::class.java)

        val bgpGlobal = writeContext.readAfter(RWUtils.cutId(instanceIdentifier, Bgp::class.java))
            .get().global
        val bgpAs = bgpGlobal?.config?.`as`!!

        val neighAfiSafi = getAfiSafisForNeighbor(bgpGlobal, neighbor)

        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
            val neighborBuilder = UnderlayNeighborBuilder()
            renderGlobalNeighbor(neighborBuilder, neighbor, neighAfiSafi)
            access.merge(getGlobalNeighborIdentifier(bgpAs, neighbor.neighborAddress.toNoZone()),
                neighborBuilder.build())
        } else {
            val neighborBuilder = UnderlayVrfNeighborBuilder()
            renderVrfNeighbor(neighborBuilder, neighbor, neighAfiSafi)
            access.merge(getVrfNeighborIdentifier(bgpAs, vrfKey, neighbor.neighborAddress.toNoZone()),
                neighborBuilder.build())
        }
    }

    override fun updateCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Neighbor>,
        dataBefore: Neighbor,
        dataAfter: Neighbor,
        writeContext: WriteContext
    ) {
        val vrfKey = instanceIdentifier.firstKeyOf(NetworkInstance::class.java)

        val bgpGlobal = writeContext.readAfter(RWUtils.cutId(instanceIdentifier, Bgp::class.java)).get().global
        val bgpAs = bgpGlobal?.config?.`as`!!

        val neighAfiSafi = getAfiSafisForNeighbor(bgpGlobal, dataAfter)

        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
            val globalNeighborIdentifier = getGlobalNeighborIdentifier(bgpAs, dataAfter.neighborAddress.toNoZone())

            val neighborBuilderBefore = UnderlayNeighborBuilder()
            val neighborBuilderAfter = UnderlayNeighborBuilder()

            renderGlobalNeighbor(neighborBuilderBefore, dataBefore, neighAfiSafi)
            renderGlobalNeighbor(neighborBuilderAfter, dataAfter, neighAfiSafi)

            access.safeMerge(globalNeighborIdentifier, neighborBuilderBefore.build(),
                globalNeighborIdentifier, neighborBuilderAfter.build())
        } else {
            val vrfNeighborIdentifier = getVrfNeighborIdentifier(bgpAs, vrfKey, dataAfter.neighborAddress.toNoZone())

            val neighborBuilderBefore = UnderlayVrfNeighborBuilder()
            val neighborBuilderAfter = UnderlayVrfNeighborBuilder()

            renderVrfNeighbor(neighborBuilderBefore, dataBefore, neighAfiSafi)
            renderVrfNeighbor(neighborBuilderAfter, dataAfter, neighAfiSafi)

            access.safeMerge(vrfNeighborIdentifier, neighborBuilderBefore.build(),
                vrfNeighborIdentifier, neighborBuilderAfter.build())
        }
    }

    override fun deleteCurrentAttributes(
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
            data: Neighbor,
            neighAfiSafi: List<Class<out AFISAFITYPE>>
        ) {
            val (asXX, asYY) = As.asToDotNotation(data.config.peerAs)

            // set update source to null
            builder.setNeighborAddress(data.neighborAddress.toNoZone())
                .setUpdateSourceInterface(null)
                .remoteAs = RemoteAsBuilder()
                            .setAsXx(BgpAsRange(asXX))
                            .setAsYy(BgpAsRange(asYY))
                            .build()

            // overwrite null if new data contains transport
            builder.updateSourceInterface = data.transport?.config?.localAddress?.toIfcName()

            // Get current Afs to map
            val currentAfs = builder
                    .neighborAfs
                    ?.neighborAf.orEmpty()
                    .map { it.afName to it }
                    .toMap()
                    .toMutableMap()

            // Rebuild AFs, use existing configuration if present
            neighAfiSafi
                    .map { it.toUnderlay() }
                    .map { it to currentAfs[it] }
                    .map { it.second ?: NeighborAfBuilder().setAfName(it.first).build() }
                    .map {
                        Pair(it.afName, NeighborAfBuilder(it)
                                .setRoutePolicyIn(data.applyPolicy?.config?.importPolicy.orEmpty().firstOrNull())
                                .setRoutePolicyOut(data.applyPolicy?.config?.exportPolicy.orEmpty().firstOrNull())
                                .setActivate(true)
                                .build())
                    }.forEach { currentAfs[it.first] = it.second }

            builder.neighborAfs = NeighborAfsBuilder()
                    .setNeighborAf(currentAfs.values.toList())
                    .build()
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
            data: Neighbor,
            neighAfiSafi: List<Class<out AFISAFITYPE>>
        ) {
            val (asXX, asYY) = As.asToDotNotation(data.config.peerAs)

            builder.setNeighborAddress(data.neighborAddress.toNoZone())
                    .setUpdateSourceInterface(data.transport?.config?.localAddress?.toIfcName()).remoteAs =
                    RemoteAsBuilder()
                            .setAsXx(BgpAsRange(asXX))
                            .setAsYy(BgpAsRange(asYY))
                            .build()

            // Get current Afs to map
            val currentAfs = builder
                    .vrfNeighborAfs
                    ?.vrfNeighborAf.orEmpty()
                    .map { it.afName to it }
                    .toMap()
                    .toMutableMap()

            // Reconfigure those coming as an update
            neighAfiSafi
                    .map { it.toUnderlay() }
                    .mapNotNull { currentAfs.getOrPut(it, { VrfNeighborAfBuilder()
                            .setAfName(it)
                            .build() }) }
                    .map {
                        Pair(it.afName, VrfNeighborAfBuilder(it)
                                .setRoutePolicyIn(data.applyPolicy?.config?.importPolicy.orEmpty().firstOrNull())
                                .setRoutePolicyOut(data.applyPolicy?.config?.exportPolicy.orEmpty().firstOrNull())
                                .setActivate(true)
                                .build())
                    }.forEach { currentAfs[it.first] = it.second }

            builder.vrfNeighborAfs = VrfNeighborAfsBuilder()
                    .setVrfNeighborAf(currentAfs.values.toList())
                    .build()
        }

        private fun getAfiSafisForNeighbor(bgpGlobal: Global, neighbor: Neighbor): List<Class<out AFISAFITYPE>> {
            return if (neighbor.afiSafis?.afiSafi.orEmpty().isNotEmpty()) {
                neighbor.afiSafis?.afiSafi.orEmpty()
                        .map { it.afiSafiName }
                        .toList()
            } else {
                bgpGlobal.afiSafis.afiSafi.orEmpty()
                        .map { it.afiSafiName }
                        .toList()
            }
        }
    }
}

private fun BgpCommonNeighborGroupTransportConfig.LocalAddress?.toIfcName(): InterfaceName? {
    return this?.string?.let {
        InterfaceName(it)
    }
}