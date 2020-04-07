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
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborWriter.Companion.NEXTHOPSELF_POLICY_NAME
import io.frinx.unitopo.unit.xr6.bgp.handler.toUnderlay
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827._default.originate.DefaultOriginateBuilder
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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.neighbor.neighbor.afs.NeighborAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.neighbor.neighbor.afs.NeighborAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfNeighbors
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighbor
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.vrf.neighbor.VrfNeighborAfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.vrf.neighbor.vrf.neighbor.afs.VrfNeighborAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.vrf.neighbor.vrf.neighbor.afs.VrfNeighborAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.ebgp.multihop.EbgpMultihopBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.maximum.prefixes.MaximumPrefixesBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.password.PasswordBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.remote.`as`.RemoteAsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.remove._private.`as`.entire.`as`.path.RemovePrivateAsEntireAsPath
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.remove._private.`as`.entire.`as`.path.RemovePrivateAsEntireAsPathBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.soft.reconfiguration.SoftReconfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.ProprietaryPassword
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.BgpNeAfAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.BgpCommonNeighborGroupTransportConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.Bgp
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.Global
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.AFISAFITYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.PRIVATEASREMOVEALL
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.PRIVATEASREPLACEALL
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.REMOVEPRIVATEASOPTION
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.IPV6UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.IPV4UNICAST
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
        const val NEXTHOPSELF_POLICY_NAME: String = "nexthopself"
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

            data.ebgpMultihop?.config?.let { builder.setEbgpMultihop(EbgpMultihopBuilder()
                .setMplsDeactivation(it.isEnabled)
                .setMaxHopCount(it.multihopTtl.toLong()).build()) }

            // overwrite null if new data contains transport
            builder.updateSourceInterface = data.transport?.config?.localAddress?.toIfcName()

            if (data.config.authPassword == null) {
                builder.password = null
            } else {
                builder.password = PasswordBuilder().apply {
                    password = ProprietaryPassword("!" + data.config.authPassword.value)
                    isPasswordDisable = false
                }.build()
            }

            builder.setShutdown(true)
            data.config?.description?.let { builder.setDescription(it) }

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
                        val afiSafi = data.afiSafis?.afiSafi
                                ?.find { afiSafi -> transformAfiToString(afiSafi.key) == it.afName.getName() }
                        val neighborAfBuilder = parseNeighborAfBuilder(data, afiSafi, it)
                        Pair(it.afName, neighborAfBuilder
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

            data.ebgpMultihop?.config?.let { builder.setEbgpMultihop(EbgpMultihopBuilder()
                .setMplsDeactivation(it.isEnabled)
                .setMaxHopCount(it.multihopTtl.toLong()).build()) }

            if (data.config.authPassword == null) {
                builder.password = null
            } else {
                builder.password = PasswordBuilder().apply {
                    password = ProprietaryPassword("!" + data.config.authPassword.value)
                    isPasswordDisable = false
                }.build()
            }

            builder.setShutdown(true)
            data.config?.description?.let { builder.setDescription(it) }

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
                        val afiSafi = data.afiSafis?.afiSafi
                                ?.find { afiSafi -> transformAfiToString(afiSafi.key) == it.afName.getName() }
                        val vrfNeighborAfBuilder = parseVrfNeighborAfBuilder(data, afiSafi, it)
                        Pair(it.afName, vrfNeighborAfBuilder
                                .setActivate(true).build())
                    }.forEach { currentAfs[it.first] = it.second }

            builder.vrfNeighborAfs = VrfNeighborAfsBuilder()
                    .setVrfNeighborAf(currentAfs.values.toList())
                    .build()
        }

        fun transformAfiToString(afiSafiKey: AfiSafiKey): String {
            // FIXME: add more if necessary
            when (afiSafiKey.afiSafiName) {
                IPV4UNICAST::class.java -> return "ipv4-unicast"
                IPV6UNICAST::class.java -> return "ipv6-unicast"
                else -> throw IllegalArgumentException("Unknown AFI/SAFI type ${afiSafiKey.afiSafiName}")
            }
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

private fun parseNeighborAfBuilder(
    data: Neighbor,
    afiSafi: AfiSafi?,
    it: NeighborAf?
): NeighborAfBuilder {
    val neighborAfBuilder = NeighborAfBuilder(it)
    setUnicast(it, afiSafi, neighborAfBuilder)
    setPolicy(data, neighborAfBuilder)
    setSoftReconfiguration(afiSafi, neighborAfBuilder)
    setRemovePrivateAs(data, neighborAfBuilder)
    setSendCommunity(data, neighborAfBuilder)
    return neighborAfBuilder
}

private fun setSendCommunity(data: Neighbor, neighborAfBuilder: NeighborAfBuilder) {
    data.config?.sendCommunity?.intValue?.let {
        neighborAfBuilder.setSendCommunityEbgp(transferCommunityType(it))
    }
}

private fun setRemovePrivateAs(data: Neighbor, neighborAfBuilder: NeighborAfBuilder) {
    val removePrivateAs = data.config?.removePrivateAs
    removePrivateAs?.let {
        neighborAfBuilder.setRemovePrivateAsEntireAsPath(transferRemovePrivateAs(data))
    }
}

private fun setSoftReconfiguration(afiSafi: AfiSafi?, neighborAfBuilder: NeighborAfBuilder) {
    val softReconfiguration = afiSafi?.config?.getAugmentation(BgpNeAfAug::class.java)?.softReconfiguration
            ?.isAlways
    softReconfiguration?.let {
        neighborAfBuilder.setSoftReconfiguration(SoftReconfigurationBuilder()
                .setSoftAlways(softReconfiguration)
                .setInboundSoft(softReconfiguration).build())
    }
}

private fun setPolicy(data: Neighbor, neighborAfBuilder: NeighborAfBuilder) {
    val applyPolicyConfig = data.applyPolicy?.config
    applyPolicyConfig?.importPolicy.orEmpty().firstOrNull()?.let { neighborAfBuilder.setRoutePolicyIn(it) }
    if (applyPolicyConfig?.exportPolicy.orEmpty().firstOrNull().equals(NEXTHOPSELF_POLICY_NAME)) {
        neighborAfBuilder.setNextHopSelf(true)
    } else {
        applyPolicyConfig?.exportPolicy.orEmpty().firstOrNull()?.let { neighborAfBuilder.setRoutePolicyOut(it) }
    }
}

private fun setUnicast(it: NeighborAf?, afiSafi: AfiSafi?, neighborAfBuilder: NeighborAfBuilder) {
    var maxPrefixes: Long? = null
    var shutdownThresholdPct: Long? = null
    var defaultOriginate: Boolean? = null

    if (it?.afName?.getName().equals("ipv4-unicast")) {
        maxPrefixes = afiSafi?.ipv4Unicast?.prefixLimit?.config?.maxPrefixes
        shutdownThresholdPct = afiSafi?.ipv4Unicast?.prefixLimit?.config?.shutdownThresholdPct?.value?.toLong()
        defaultOriginate = afiSafi?.ipv4Unicast?.config?.isSendDefaultRoute
    } else if (it?.afName?.getName().equals("ipv6-unicast")) {
        maxPrefixes = afiSafi?.ipv6Unicast?.prefixLimit?.config?.maxPrefixes
        shutdownThresholdPct = afiSafi?.ipv6Unicast?.prefixLimit?.config?.shutdownThresholdPct?.value?.toLong()
        defaultOriginate = afiSafi?.ipv6Unicast?.config?.isSendDefaultRoute
    }
    maxPrefixes?.let { maxPrefix ->
        val maximumPrefixesBuilder = MaximumPrefixesBuilder()
        maximumPrefixesBuilder.setPrefixLimit(maxPrefix)
        shutdownThresholdPct?.let { shutdownThresholdPct ->
            maximumPrefixesBuilder.setWarningPercentage(shutdownThresholdPct)
        }
        neighborAfBuilder.setMaximumPrefixes(maximumPrefixesBuilder.build())
    }
    defaultOriginate?.let {
        neighborAfBuilder.setDefaultOriginate(DefaultOriginateBuilder().setEnable(defaultOriginate).build())
    }
}

private fun parseVrfNeighborAfBuilder(
    data: Neighbor,
    afiSafi: AfiSafi?,
    it: VrfNeighborAf?
): VrfNeighborAfBuilder {
    val vrfNeighborAfBuilder = VrfNeighborAfBuilder(it)
    setVrfUnicast(it, afiSafi, vrfNeighborAfBuilder)
    setVrfPolicy(data, vrfNeighborAfBuilder)
    setVrfSoftReconfiguration(afiSafi, vrfNeighborAfBuilder)
    setVrfRemovePrivateAs(data, vrfNeighborAfBuilder)
    setSendCommunity(data, vrfNeighborAfBuilder)
    return vrfNeighborAfBuilder
}

private fun setSendCommunity(data: Neighbor, vrfNeighborAfBuilder: VrfNeighborAfBuilder) {
    data.config?.sendCommunity?.intValue?.let {
        vrfNeighborAfBuilder.setSendCommunityEbgp(transferCommunityType(it))
    }
}

fun setVrfRemovePrivateAs(data: Neighbor, vrfNeighborAfBuilder: VrfNeighborAfBuilder) {
    val removePrivateAs = data.config?.removePrivateAs
    removePrivateAs?.let {
        vrfNeighborAfBuilder.setRemovePrivateAsEntireAsPath(transferRemovePrivateAs(data))
    }
}

fun setVrfSoftReconfiguration(afiSafi: AfiSafi?, vrfNeighborAfBuilder: VrfNeighborAfBuilder) {
    val softReconfiguration = afiSafi?.config?.getAugmentation(BgpNeAfAug::class.java)?.softReconfiguration
            ?.isAlways
    softReconfiguration?.let {
        vrfNeighborAfBuilder.setSoftReconfiguration(SoftReconfigurationBuilder()
                .setSoftAlways(softReconfiguration)
                .setInboundSoft(softReconfiguration).build())
    }
}

fun setVrfPolicy(data: Neighbor, vrfNeighborAfBuilder: VrfNeighborAfBuilder) {
    val applyPolicyConfig = data.applyPolicy?.config
    applyPolicyConfig?.importPolicy.orEmpty().firstOrNull()?.let { vrfNeighborAfBuilder.setRoutePolicyIn(it) }
    if (applyPolicyConfig?.exportPolicy.orEmpty().firstOrNull().equals(NEXTHOPSELF_POLICY_NAME)) {
        vrfNeighborAfBuilder.setNextHopSelf(true)
    } else {
        applyPolicyConfig?.exportPolicy.orEmpty().firstOrNull()?.let { vrfNeighborAfBuilder.setRoutePolicyOut(it) }
    }
}

fun setVrfUnicast(it: VrfNeighborAf?, afiSafi: AfiSafi?, vrfNeighborAfBuilder: VrfNeighborAfBuilder) {
    var maxPrefixes: Long? = null
    var shutdownThresholdPct: Long? = null
    var defaultOriginate: Boolean? = null

    if (it?.afName?.getName().equals("ipv4-unicast")) {
        maxPrefixes = afiSafi?.ipv4Unicast?.prefixLimit?.config?.maxPrefixes
        shutdownThresholdPct = afiSafi?.ipv4Unicast?.prefixLimit?.config?.shutdownThresholdPct?.value?.toLong()
        defaultOriginate = afiSafi?.ipv4Unicast?.config?.isSendDefaultRoute
    } else if (it?.afName?.getName().equals("ipv6-unicast")) {
        maxPrefixes = afiSafi?.ipv6Unicast?.prefixLimit?.config?.maxPrefixes
        shutdownThresholdPct = afiSafi?.ipv6Unicast?.prefixLimit?.config?.shutdownThresholdPct?.value?.toLong()
        defaultOriginate = afiSafi?.ipv6Unicast?.config?.isSendDefaultRoute
    }
    maxPrefixes?.let { maxPrefix ->
        val maximumPrefixesBuilder = MaximumPrefixesBuilder()
        maximumPrefixesBuilder.setPrefixLimit(maxPrefix)
        shutdownThresholdPct?.let { shutdownThresholdPct ->
            maximumPrefixesBuilder.setWarningPercentage(shutdownThresholdPct)
        }
        vrfNeighborAfBuilder.setMaximumPrefixes(maximumPrefixesBuilder.build())
    }
    defaultOriginate?.let {
        vrfNeighborAfBuilder.setDefaultOriginate(DefaultOriginateBuilder().setEnable(defaultOriginate).build())
    }
}

private fun transferRemovePrivateAs(data: Neighbor): RemovePrivateAsEntireAsPath {
    val removePrivateAsEntireAsPathBuilder = RemovePrivateAsEntireAsPathBuilder()
    when (data.config.removePrivateAs) {
        REMOVEPRIVATEASOPTION::class.java -> removePrivateAsEntireAsPathBuilder.setEnable(true).setEntire(true)
        PRIVATEASREMOVEALL::class.java -> removePrivateAsEntireAsPathBuilder.setEnable(true).setEntire(false)
        PRIVATEASREPLACEALL::class.java -> removePrivateAsEntireAsPathBuilder.setEnable(false).setEntire(false)
        else -> throw IllegalArgumentException("Unknown removePrivateAs type ${data.config.removePrivateAs}")
    }
    return removePrivateAsEntireAsPathBuilder.build()
}

private fun transferCommunityType(communityType: Int): Boolean {
    if (communityType in 0..3)
        return true
    return false
}