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

package io.frinx.unitopo.unit.xr6.vrf

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.common.BgpListWriter
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.Bgp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.BgpBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.BgpRouteDistinguisher
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.DefaultVrfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.VrfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.GlobalAfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.global.afs.GlobalAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.global.afs.GlobalAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.global.afs.GlobalAfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.VrfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfGlobalBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.RouteDistinguisherBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.VrfGlobalAfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.vrf.global.afs.VrfGlobalAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.vrf.global.afs.VrfGlobalAfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpExtcommAsnIndex
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.Global
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.IPV4UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.L3VPNIPV4UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.slf4j.LoggerFactory
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.Config as ProtoConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.Config as GlobalConfig
import java.util.*
import java.util.regex.Pattern
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class AfiSafilWriter(private val underlayAccess: UnderlayAccess) : BgpListWriter<AfiSafi, AfiSafiKey> {

    override fun updateCurrentAttributesForType(iid: IID<AfiSafi>, dataBefore: AfiSafi, dataAfter: AfiSafi, writeContext: WriteContext) {
        deleteCurrentAttributesForType(iid, dataBefore, writeContext)
        writeCurrentAttributesForType(iid, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributesForType(iid: IID<AfiSafi>, dataBefore: AfiSafi, wtc: WriteContext) {
        val bgpIid = IID.create(Bgp::class.java)

        try {
            underlayAccess.delete(bgpIid)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(bgpIid, e)
        }

    }

    override fun writeCurrentAttributesForType(iid: IID<AfiSafi>, dataAfter: AfiSafi, wtc: WriteContext) {
        val bgpIid = IID.create(Bgp::class.java)
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        val rd = wtc.readAfter(IID.create(NetworkInstances::class.java)
                .child(NetworkInstance::class.java, NetworkInstanceKey(vrfName))
                .child(Config::class.java)).get().routeDistinguisher.string
        //TODO use subtree writer for continer global, that will remove need to read config container
        val cfg = wtc.readAfter(iid.firstIdentifierOf(Global::class.java)
                .child(GlobalConfig::class.java)).get()
        val bgp = getBgpData(dataAfter, cfg, vrfName, rd)

        try {
            underlayAccess.merge(bgpIid, bgp)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(bgpIid, e)
        }
    }

    private fun getBgpData(afisafi: AfiSafi, cfg: GlobalConfig, vrfName: String, rd: String): Bgp {
        val bgp = BgpBuilder()
                .setInstance(Arrays.asList(
                        InstanceBuilder()
                                .setKey(InstanceKey(CiscoIosXrString("default")))
                                .setInstanceAs(Arrays.asList(
                                        InstanceAsBuilder()
                                                .setKey(InstanceAsKey(BgpAsRange(0)))
                                                .setFourByteAs(Arrays.asList(
                                                        FourByteAsBuilder()
                                                                .setAs(BgpAsRange(cfg.`as`?.value))
                                                                .setBgpRunning(true)
                                                                .let {
                                                                    if(!vrfName.equals("default")) {
                                                                        it.vrfs = VrfsBuilder()
                                                                                .setVrf(afiSafiToVrf(afisafi, vrfName, rd))
                                                                                .build()
                                                                        it.defaultVrf = DefaultVrfBuilder()
                                                                                .setGlobal(GlobalBuilder()
                                                                                        .setGlobalAfs(GlobalAfsBuilder()
                                                                                                .setGlobalAf(Arrays.asList(GlobalAfBuilder()
                                                                                                        .setKey(GlobalAfKey(BgpAddressFamily.VpNv4Unicast))
                                                                                                        .setEnable(true)
                                                                                                        .build()))
                                                                                                .build())
                                                                                        .build())
                                                                                .build()
                                                                    } else {

                                                                        it.defaultVrf = DefaultVrfBuilder()
                                                                                .setGlobal(GlobalBuilder()
                                                                                        .setGlobalAfs(GlobalAfsBuilder()
                                                                                                .setGlobalAf(afiSafiToGlobalAf(afisafi))
                                                                                                .build())
                                                                                        .build())
                                                                                .build()
                                                                    }
                                                                    it
                                                                }
                                                                .build()
                                                ))
                                                .build()
                                ))
                                .build()
                ))

        return bgp.build()
    }

    private fun afiSafiToVrf(afiSafi: AfiSafi, vrfName: String, rd: String): List<Vrf> {
        val pattern = Pattern.compile("(?<prefix>.+):(?<suffix>.+)")
        val matcher = pattern.matcher(rd)
        matcher.matches()
        val asPrefix = matcher.group("prefix")
        val asSuffix = matcher.group("suffix")
        val vrfList = ArrayList<Vrf>()
        val vrfGlobalAfKey: VrfGlobalAfKey
        if (afiSafi.config?.afiSafiName!!.equals(IPV4UNICAST::class.java)) {
            vrfGlobalAfKey = VrfGlobalAfKey(BgpAddressFamily.Ipv4Unicast)
        } else {
            return vrfList
        }
        vrfList.add(VrfBuilder()
                .setKey(VrfKey(CiscoIosXrString(vrfName)))
                .setVrfGlobal(VrfGlobalBuilder()
                        .setExists(true)
                        .setRouteDistinguisher(RouteDistinguisherBuilder()
                                .setType(BgpRouteDistinguisher.As)
                                .setAsXx(BgpAsRange(0))
                                .setAs(BgpAsRange(asPrefix.toLong()))
                                .setAsIndex(BgpExtcommAsnIndex(asSuffix.toLong()))
                                .build())
                        .setVrfGlobalAfs(VrfGlobalAfsBuilder()
                                .setVrfGlobalAf(Arrays.asList(VrfGlobalAfBuilder()
                                        .setKey(vrfGlobalAfKey)
                                        .setEnable(afiSafi.config.isEnabled)
                                        .build()))
                                .build())
                        .build())
                .build())
        return vrfList
    }

    private fun afiSafiToGlobalAf(afiSafi: AfiSafi): List<GlobalAf> {
        val afList = ArrayList<GlobalAf>()
        val globalAfKey: GlobalAfKey
        if (afiSafi.config?.afiSafiName!!.equals(IPV4UNICAST::class.java)) {
            globalAfKey = GlobalAfKey(BgpAddressFamily.Ipv4Unicast)
        } else if (afiSafi.config?.afiSafiName!!.equals(L3VPNIPV4UNICAST::class.java)) {
            globalAfKey = GlobalAfKey(BgpAddressFamily.VpNv4Unicast)
        } else {
            return afList
        }
        afList.add(GlobalAfBuilder()
                .setKey(globalAfKey)
                .setEnable(afiSafi.config.isEnabled)
                .build())
        return afList
    }
}