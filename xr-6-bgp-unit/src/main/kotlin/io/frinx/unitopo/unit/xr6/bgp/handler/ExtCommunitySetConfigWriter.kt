/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.common.BgpWriter
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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ExtCommunitySets
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ext.community.set.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.routing.policy.defined.sets.BgpDefinedSets
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import java.util.*
import java.util.regex.Pattern
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class ExtCommunitySetConfigWriter(private val underlayAccess: UnderlayAccess) : BgpWriter<Config> {

    val importPattern = Pattern.compile("(?<vrf>.+)-route-target-import-set")
    val exportPattern = Pattern.compile("(?<vrf>.+)-route-target-export-set")
    val routeTargetPattern = Pattern.compile("(?<as>.+):(?<asIndex>.+)")

    override fun updateCurrentAttributesForType(iid: IID<Config>, dataBefore: Config, dataAfter: Config, writeContext: WriteContext) {
        deleteCurrentAttributesForType(iid, dataBefore, writeContext)
        writeCurrentAttributesForType(iid, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributesForType(iid: IID<Config>, dataBefore: Config, wtc: WriteContext) {
        val bgpIid = getBgpIid(iid.firstKeyOf(NetworkInstance::class.java).name)
        val importMatcher = importPattern.matcher(dataBefore.extCommunitySetName)
        val exportMatcher = exportPattern.matcher(dataBefore.extCommunitySetName)

        if (importMatcher.matches()) {
            dataBefore.extCommunityMember?.forEach {
                val asMatcher = routeTargetPattern.matcher(String(it.value))
                val asNumber = asMatcher.group("as")
                val asIndex = asMatcher.group("asIndex")

                val deleteIid = bgpIid.child(ImportRouteTargets::class.java)
                        .child(RouteTargets::class.java)
                        .child(RouteTarget::class.java, RouteTargetKey(BgpVrfRouteTarget.As))
                        .child(AsOrFourByteAs::class.java, AsOrFourByteAsKey(asNumber.toLong(),
                                RouteTargetAsIndex(asIndex.toLong()),
                                0,0))
                try {
                    underlayAccess.delete(deleteIid)
                } catch (e: Exception) {
                    throw io.fd.honeycomb.translate.write.WriteFailedException(deleteIid, e)
                }
            }
        } else if (exportMatcher.matches()) {
            dataBefore.extCommunityMember?.forEach {
                val asMatcher = routeTargetPattern.matcher(String(it.value))
                val asNumber = asMatcher.group("as")
                val asIndex = asMatcher.group("asIndex")

                val deleteIid = bgpIid.child(ExportRouteTargets::class.java)
                        .child(RouteTargets::class.java)
                        .child(RouteTarget::class.java, RouteTargetKey(BgpVrfRouteTarget.As))
                        .child(AsOrFourByteAs::class.java, AsOrFourByteAsKey(asNumber.toLong(),
                                RouteTargetAsIndex(asIndex.toLong()),
                                0, 0))
                try {
                    underlayAccess.delete(deleteIid)
                } catch (e: Exception) {
                    throw io.fd.honeycomb.translate.write.WriteFailedException(deleteIid, e)
                }
            }
        }
    }

    override fun writeCurrentAttributesForType(iid: IID<Config>, dataAfter: Config, wtc: WriteContext) {
        val vrfName =  iid.firstKeyOf(NetworkInstance::class.java).name
        val bgpIid = getBgpIid(vrfName)
        val extCommSets = wtc.readAfter(IID.create(BgpDefinedSets::class.java)
                .child(ExtCommunitySets::class.java))
                .get()

        val bgp = getBgpData(dataAfter)

        try {
            underlayAccess.merge(bgpIid, bgp)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(bgpIid, e)
        }
    }

    private fun getBgpData(data: Config): Bgp {
        val importMatcher = importPattern.matcher(data.extCommunitySetName)
        val exportMatcher = exportPattern.matcher(data.extCommunitySetName)
        val bgp = BgpBuilder()
                .let({
                    if (importMatcher.matches()) {
                        it.importRouteTargets = ImportRouteTargetsBuilder()
                                .setRouteTargets(RouteTargetsBuilder()
                                        .setRouteTarget(Arrays.asList(RouteTargetBuilder()
                                                .setKey(RouteTargetKey(BgpVrfRouteTarget.As))
                                                .setAsOrFourByteAs(extCommunityMemberToAsOrFourByteAs(data.extCommunityMember!!))
                                                .build())
                                        )
                                        .build())
                                .build()
                    } else if (exportMatcher.matches()) {
                        it.exportRouteTargets = ExportRouteTargetsBuilder()
                                .setRouteTargets(RouteTargetsBuilder()
                                        .setRouteTarget(Arrays.asList(RouteTargetBuilder()
                                                .setKey(RouteTargetKey(BgpVrfRouteTarget.As))
                                                .setAsOrFourByteAs(extCommunityMemberToAsOrFourByteAs(data.extCommunityMember!!))
                                                .build())
                                        )
                                        .build())
                                .build()
                    }
                    it
                })

        return bgp.build()
    }

    private fun extCommunityMemberToAsOrFourByteAs(members: List<ExtCommunitySetConfig.ExtCommunityMember>): List<AsOrFourByteAs> {
        val list = ArrayList<AsOrFourByteAs>()
        members.forEach {
            list.add(AsOrFourByteAsBuilder()
                    .setKey(AsOrFourByteAsKey(
                            routeTargetPattern.matcher(String(it.value))
                                    .group("as")
                                    .toLong(),
                            RouteTargetAsIndex(routeTargetPattern.matcher(String(it.value))
                                    .group("asIndex")
                                    .toLong()),
                            0, 0))
                    .build())
        }
        return list
    }

    companion object {
        public fun getBgpIid(vrfName: String): IID<Bgp> {
            return IID.create(Vrfs::class.java)
                    .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfName)))
                    .child(Afs::class.java)
                    .child(Af::class.java, AfKey(VrfAddressFamily.Ipv4, VrfSubAddressFamily.Unicast, CiscoIosXrString("default")))
                    .augmentation(Af1::class.java)
                    .child(Bgp::class.java)
        }
    }
}