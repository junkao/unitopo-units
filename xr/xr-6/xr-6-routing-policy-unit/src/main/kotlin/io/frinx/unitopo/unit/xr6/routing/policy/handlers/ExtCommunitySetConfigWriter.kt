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

package io.frinx.unitopo.unit.xr6.routing.policy.handlers

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.VrfAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.VrfSubAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.af.table.Afs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.af.table.afs.Af
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.af.table.afs.AfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.Af1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.BgpVrfRouteTarget
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.RouteTargetAsIndex
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.route.target.table.RouteTargets
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.route.target.table.RouteTargetsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.route.target.table.route.targets.RouteTarget
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.route.target.table.route.targets.RouteTargetBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.route.target.table.route.targets.RouteTargetKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.route.target.table.route.targets.route.target.AsOrFourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.route.target.table.route.targets.route.target.AsOrFourByteAsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.route.target.table.route.targets.route.target.AsOrFourByteAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.vrfs.vrf.afs.af.Bgp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.vrfs.vrf.afs.af.BgpBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.vrfs.vrf.afs.af.bgp.ExportRouteTargets
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.vrfs.vrf.afs.af.bgp.ExportRouteTargetsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.vrfs.vrf.afs.af.bgp.ImportRouteTargets
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.vrfs.vrf.afs.af.bgp.ImportRouteTargetsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ExtCommunitySetConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ext.community.set.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.IPV4
import java.util.Arrays
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class ExtCommunitySetConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun updateCurrentAttributes(
        iid: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        deleteCurrentAttributes(iid, dataBefore, writeContext)
        writeCurrentAttributes(iid, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributes(iid: IID<Config>, dataBefore: Config, wtc: WriteContext) {
        val importMatcher = IMPORT_TARGET_PATTERN.matcher(dataBefore.extCommunitySetName)
        val exportMatcher = EXPORT_TARGET_PATTERN.matcher(dataBefore.extCommunitySetName)

        val vrfName = if (importMatcher.matches()) importMatcher.group("vrf") else {
            exportMatcher.matches()

            exportMatcher.group("vrf")
        }

        val bgpIid = getBgpIid(vrfName)

        if (importMatcher.matches()) {
            dataBefore.extCommunityMember.orEmpty()
                    .map { ROUTE_TARGET.matcher(String(it.value)) }
                    .filter { it.matches() }
                    .forEach {
                        val asNumber = it.group("as")
                        val asIndex = it.group("asIndex")

                        val deleteIid = bgpIid.child(ImportRouteTargets::class.java)
                                .child(RouteTargets::class.java)
                                .child(RouteTarget::class.java, RouteTargetKey(BgpVrfRouteTarget.As))
                                .child(AsOrFourByteAs::class.java, AsOrFourByteAsKey(asNumber.toLong(),
                                        RouteTargetAsIndex(asIndex.toLong()),
                                        0, 0))
                        underlayAccess.delete(deleteIid)
                    }
        } else if (exportMatcher.matches()) {
            dataBefore.extCommunityMember.orEmpty()
                    .map { ROUTE_TARGET.matcher(String(it.value)) }
                    .filter { it.matches() }
                    .forEach {
                        val asNumber = it.group("as")
                        val asIndex = it.group("asIndex")

                        val deleteIid = bgpIid.child(ExportRouteTargets::class.java)
                                .child(RouteTargets::class.java)
                                .child(RouteTarget::class.java, RouteTargetKey(BgpVrfRouteTarget.As))
                                .child(AsOrFourByteAs::class.java, AsOrFourByteAsKey(asNumber.toLong(),
                                        RouteTargetAsIndex(asIndex.toLong()),
                                        0, 0))
                        underlayAccess.delete(deleteIid)
                    }
        }
    }

    override fun writeCurrentAttributes(iid: IID<Config>, data: Config, wtc: WriteContext) {
        val importMatcher = IMPORT_TARGET_PATTERN.matcher(data.extCommunitySetName)
        val exportMatcher = EXPORT_TARGET_PATTERN.matcher(data.extCommunitySetName)

        require(importMatcher.matches() || exportMatcher.matches(),
                { "Invalid ext community: ${data.extCommunitySetName}. Expected communities are in format: " +
                    "$IMPORT_TARGET_PATTERN or $EXPORT_TARGET_PATTERN" })

        val vrfName = if (importMatcher.matches()) importMatcher.group("vrf") else exportMatcher.group("vrf")

        val enabledAfis = wtc.readAfter(IIDs.NETWORKINSTANCES.child(NetworkInstance::class.java,
            NetworkInstanceKey(vrfName)))
                .orNull()
                ?.config
                ?.enabledAddressFamilies.orEmpty()

        require(enabledAfis.isNotEmpty(),
                { "No enabled address family for VRF: $vrfName" })

        // TODO only Ipv4 supported
        requireNotNull(enabledAfis.find { it == IPV4::class.java },
                { "IPv4 is not among enabled address families for vrf: $vrfName" })

        val bgpIid = getBgpIid(vrfName)

        val bgp = getBgpData(data, importMatcher, exportMatcher)

        underlayAccess.merge(bgpIid, bgp)
    }

    private fun getBgpData(data: Config, importMatcher: Matcher, exportMatcher: Matcher): Bgp {

        val bgp = BgpBuilder()
                .let({
                    if (importMatcher.matches()) {
                        it.importRouteTargets = ImportRouteTargetsBuilder()
                                .setRouteTargets(RouteTargetsBuilder()
                                        .setRouteTarget(Arrays.asList(RouteTargetBuilder()
                                                .setKey(RouteTargetKey(BgpVrfRouteTarget.As))
                                                .setAsOrFourByteAs(extCommunityMemberToAsOrFourByteAs(
                                                    data.extCommunityMember!!))
                                                .build())
                                        )
                                        .build())
                                .build()
                    } else if (exportMatcher.matches()) {
                        it.exportRouteTargets = ExportRouteTargetsBuilder()
                                .setRouteTargets(RouteTargetsBuilder()
                                        .setRouteTarget(Arrays.asList(RouteTargetBuilder()
                                                .setKey(RouteTargetKey(BgpVrfRouteTarget.As))
                                                .setAsOrFourByteAs(extCommunityMemberToAsOrFourByteAs(
                                                    data.extCommunityMember!!))
                                                .build())
                                        )
                                        .build())
                                .build()
                    }
                    it
                })

        return bgp.build()
    }

    private fun extCommunityMemberToAsOrFourByteAs(
        members: List<ExtCommunitySetConfig.ExtCommunityMember>
    ): List<AsOrFourByteAs> {
        return members
                .map { ROUTE_TARGET.matcher(String(it.value)) }
                .filter { it.matches() }
                .map {
                    AsOrFourByteAsBuilder()
                            .setKey(AsOrFourByteAsKey(
                                    it.group("as")
                                            .toLong(),
                                    RouteTargetAsIndex(it
                                            .group("asIndex")
                                            .toLong()),
                                    0, 0))
                            .build()
                }
    }

    companion object {
        val IMPORT_TARGET_PATTERN = Pattern.compile("(?<vrf>.+)-route-target-import-set")
        val EXPORT_TARGET_PATTERN = Pattern.compile("(?<vrf>.+)-route-target-export-set")
        val ROUTE_TARGET = Pattern.compile("(?<as>.+):(?<asIndex>.+)")

        public fun getBgpIid(vrfName: String): IID<Bgp> {

            return IID.create(Vrfs::class.java)
                    .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfName)))
                    .child(Afs::class.java)
                    .child(Af::class.java, AfKey(VrfAddressFamily.Ipv4, VrfSubAddressFamily.Unicast,
                        CiscoIosXrString("default")))
                    .augmentation(Af1::class.java)
                    .child(Bgp::class.java)
        }
    }
}