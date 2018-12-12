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

package io.frinx.unitopo.unit.xr6.bgp.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.As.Companion.asToDotNotation
import io.frinx.unitopo.handlers.bgp.BgpReader
import io.frinx.unitopo.unit.xr6.bgp.IID
import io.frinx.unitopo.unit.xr6.bgp.UnderlayBgp
import io.frinx.unitopo.unit.xr6.bgp.UnderlayBgpBuilder
import io.frinx.unitopo.unit.xr6.bgp.UnderlayRouteDistinguisher
import io.frinx.unitopo.unit.xr6.bgp.UnderlayRouteDistinguisherBuilder
import io.frinx.unitopo.handlers.bgp.BgpWriter
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.BgpRouteDistinguisher
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.DefaultVrfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.Global
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.VrfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfGlobal
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfGlobalBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpExtcommAsnIndex
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpExtcommV4AddrIndex
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.Bgp
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.RouteDistinguisher
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.regex.Pattern
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config as NetworkInstanceConfig

class GlobalConfigWriter(private val underlayAccess: UnderlayAccess) : BgpWriter<Config> {

    override fun updateCurrentAttributesForType(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val vrfKey = checkArguments(id)

        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {

            val bgpBuilder = underlayAccess.read(XR_BGP_ID)
                    .checkedGet()
                    .or({ XR_EMPTY_BGP })
                    .let { UnderlayBgpBuilder(it) }

            renderGlobalData(bgpBuilder, dataAfter)
            underlayAccess.put(XR_BGP_ID, bgpBuilder.build())
        } else {
            val vrfId = getVrfId(vrfKey, dataAfter.`as`)
            val rd = writeContext.readAfter(InstanceIdentifier.create(NetworkInstances::class.java)
                    .child(NetworkInstance::class.java, NetworkInstanceKey(vrfKey.name))
                    .child(NetworkInstanceConfig::class.java)).orNull()?.routeDistinguisher

            val bgpBuilder = underlayAccess.read(vrfId)
                    .checkedGet()
                    .or({ XR_EMPTY_VRF_BGP })
                    .let { VrfBuilder(it) }

            renderVrfData(bgpBuilder, vrfKey, dataAfter, rd)
            underlayAccess.put(vrfId, bgpBuilder.build())
        }
    }

    override fun writeCurrentAttributesForType(id: IID<Config>, dataAfter: Config, wc: WriteContext) {
        val vrfKey = checkArguments(id)

        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
            val bgpBuilder = UnderlayBgpBuilder()
            renderGlobalData(bgpBuilder, dataAfter)
            underlayAccess.put(XR_BGP_ID, bgpBuilder.build())
        } else {
            val rd = wc.readAfter(InstanceIdentifier.create(NetworkInstances::class.java)
                    .child(NetworkInstance::class.java, NetworkInstanceKey(vrfKey.name))
                    .child(NetworkInstanceConfig::class.java)).orNull()?.routeDistinguisher

            val bgpBuilder = VrfBuilder()
            renderVrfData(bgpBuilder, vrfKey, dataAfter, rd)
            underlayAccess.merge(getVrfId(vrfKey, dataAfter.`as`), bgpBuilder.build())
        }
    }

    private fun checkArguments(id: IID<Config>): NetworkInstanceKey {
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val protoKey = id.firstKeyOf(Protocol::class.java)
        require(protoKey.name == BgpReader.NAME,
                { "BGP protocol instance has to be named: ${BgpReader.NAME}. Not: $protoKey" })
        return vrfKey
    }

    override fun deleteCurrentAttributesForType(iid: IID<Config>, dataBefore: Config, wc: WriteContext) {
        val vrfKey = iid.firstKeyOf(NetworkInstance::class.java)
        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
            underlayAccess.delete(XR_BGP_ID)
        } else {
            underlayAccess.delete(getVrfId(vrfKey, dataBefore.`as`))
        }
    }

    companion object {
        val XR_BGP_INSTANCE_NAME = CiscoIosXrString(BgpReader.NAME)
        val XR_BGP_ID = IID.create(UnderlayBgp::class.java)
        private val XR_EMPTY_BGP = UnderlayBgpBuilder().build()
        private val XR_EMPTY_VRF_BGP = VrfBuilder().build()

        public fun getVrfId(vrfKey: NetworkInstanceKey, asNum: AsNumber): IID<Vrf> {
            val (as1, as2) = asToDotNotation(asNum)

            return XR_BGP_ID.child(Instance::class.java, InstanceKey(XR_BGP_INSTANCE_NAME))
                    .child(InstanceAs::class.java, InstanceAsKey(BgpAsRange(as1)))
                    .child(FourByteAs::class.java, FourByteAsKey(BgpAsRange(as2)))
                    .child(Vrfs::class.java)
                    .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfKey.name)))
        }

        private fun renderVrfData(
            bgpBuilder: VrfBuilder,
            vrfKey: NetworkInstanceKey,
            dataAfter: Config,
            rd: RouteDistinguisher?
        ) {
            // Reuse existing fields for global container
            val globalBuilder = bgpBuilder.vrfGlobal?.let {
                VrfGlobalBuilder(it)
            } ?: VrfGlobalBuilder()

            bgpBuilder
                    .setKey(VrfKey(CiscoIosXrString(vrfKey.name)))
                    .setVrfName(CiscoIosXrString(vrfKey.name))
                    .setVrfGlobal(dataAfter.getVrfGlobal(globalBuilder, rd))
                    .build()
        }

        private fun renderGlobalData(bgpBuilder: UnderlayBgpBuilder, dataAfter: Config) {
            val (as1, as2) = asToDotNotation(dataAfter.`as`)

            // Reuse existing fields for four byte as container
            val fourByteAsBuilder = bgpBuilder.instance.orEmpty().firstOrNull()
                    ?.instanceAs.orEmpty().firstOrNull()
                    ?.fourByteAs.orEmpty().firstOrNull()
                    ?.let {
                        FourByteAsBuilder(it)
                    } ?: FourByteAsBuilder()

            // Reuse existing fields for global container
            val globalBuilder = bgpBuilder.instance.orEmpty().firstOrNull()
                    ?.instanceAs.orEmpty().firstOrNull()
                    ?.fourByteAs.orEmpty().firstOrNull()
                    ?.defaultVrf
                    ?.global
                    ?.let {
                        GlobalBuilder(it)
                    } ?: GlobalBuilder()

            bgpBuilder
                    .setInstance(listOf(InstanceBuilder()
                            .setInstanceName(XR_BGP_INSTANCE_NAME)
                            .setInstanceAs(listOf(InstanceAsBuilder()
                                    .setAs(BgpAsRange(as1))
                                    .setFourByteAs(listOf(fourByteAsBuilder
                                            .setBgpRunning(true)
                                            .setAs(BgpAsRange(as2))
                                            .setDefaultVrf(DefaultVrfBuilder()
                                                    .setGlobal(dataAfter.getGlobal(globalBuilder))
                                                    .build())
                                            .build()))
                                    .build()))
                            .build()))
                    .build()
        }
    }
}

private val RD_COLON_PATTERN = Pattern.compile("(?<prefix>\\d+):(?<suffix>\\d+)")!!
private val RD_IP_PATTERN = Pattern.compile("(?<prefix>\\d+\\.\\d+\\.\\d+\\.\\d+):(?<suffix>\\d+)")!!

fun RouteDistinguisher.toXrRouteDistinguisher(): UnderlayRouteDistinguisher {
    val colonMatcher = RD_COLON_PATTERN.matcher(string)
    val ipMatcher = RD_IP_PATTERN.matcher(string)

    return when {
        string == "auto" -> UnderlayRouteDistinguisherBuilder()
                .setType(BgpRouteDistinguisher.Auto)
                .build()

        colonMatcher.matches() -> {
            val asPrefix = colonMatcher.group("prefix")
            val asSuffix = colonMatcher.group("suffix")

            val (as1, as2) = asToDotNotation(AsNumber(asPrefix.toLong()))
            UnderlayRouteDistinguisherBuilder()
                    .setType(if (as1 > 0) BgpRouteDistinguisher.FourByteAs else BgpRouteDistinguisher.As)
                    .setAsXx(BgpAsRange(as1))
                    .setAs(BgpAsRange(as2))
                    .setAsIndex(BgpExtcommAsnIndex(asSuffix.toLong()))
                    .build()
        }

        ipMatcher.matches() -> {
            val asPrefix = ipMatcher.group("prefix")
            val asSuffix = ipMatcher.group("suffix")

            UnderlayRouteDistinguisherBuilder()
                    .setType(BgpRouteDistinguisher.Ipv4Address)
                    .setAddress(Ipv4AddressNoZone(asPrefix))
                    .setAddressIndex(BgpExtcommV4AddrIndex(asSuffix.toLong()))
                    .build()
        }
        else -> {
            throw IllegalArgumentException("Unable to process rd: $string, Unsupported format")
        }
    }
}

private fun Config.getGlobal(globalBuilder: GlobalBuilder): Global {
    return globalBuilder
            // optional
            .setRouterId(getXrRouterId())
            .build()
}

private fun Config.getVrfGlobal(globalBuilder: VrfGlobalBuilder, rd: RouteDistinguisher?): VrfGlobal {
    return globalBuilder
            // optional
            .setRouterId(getXrRouterId())
            .setRouteDistinguisher(rd?.toXrRouteDistinguisher())
            .setExists(true)
            .build()
}

private fun Config.getXrRouterId() =
        if (routerId?.value != null) Ipv4AddressNoZone(routerId.value) else null

/**
 * Collect all afi safi referenced in this instance
 */
public fun Bgp.getAfiSafis(): Set<AfiSafi> {
    val global = this
            .global
            ?.afiSafis
            ?.afiSafi.orEmpty()
            .map { AfiSafiBuilder().setAfiSafiName(it.afiSafiName).build() }
            .toSet()
            .toMutableSet()

    this.neighbors
            ?.neighbor.orEmpty()
            .flatMap { it.afiSafis?.afiSafi.orEmpty() }
            .map { AfiSafiBuilder().setAfiSafiName(it.afiSafiName).build() }
            .forEach { global.add(it) }

    return global
}