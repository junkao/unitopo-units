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

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.routing.policy.handlers.ExtCommunitySetReader.Companion.ROUTE_TARGET_EXPORT_SET
import io.frinx.unitopo.unit.xr6.routing.policy.handlers.ExtCommunitySetReader.Companion.ROUTE_TARGET_IMPORT_SET
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.VrfAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.VrfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.Af1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.BgpVrfRouteTarget
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.ROUTETARGETTABLE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ExtCommunitySetConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ExtCommunitySetsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ExtCommunitySet
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ExtCommunitySetBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ExtCommunitySetKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ext.community.set.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.BgpExtCommunityType
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class ExtCommunitySetReader(
    private val underlayAccess: UnderlayAccess
) : ConfigListReaderCustomizer<ExtCommunitySet, ExtCommunitySetKey, ExtCommunitySetBuilder> {

    override fun readCurrentAttributes(
        id: InstanceIdentifier<ExtCommunitySet>,
        builder: ExtCommunitySetBuilder,
        ctx: ReadContext
    ) {
        val vrfs = underlayAccess.read(VRFS_ID, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .or(EMPTY_VRFS)

        parseCurrentAttributes(vrfs, builder, id.firstKeyOf(ExtCommunitySet::class.java).extCommunitySetName)
    }

    override fun getAllIds(id: InstanceIdentifier<ExtCommunitySet>, context: ReadContext): List<ExtCommunitySetKey> {
        val vrfs = underlayAccess.read(VRFS_ID, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .or(EMPTY_VRFS)

        return parseAllIds(vrfs)
    }

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<ExtCommunitySet>) {
        (builder as ExtCommunitySetsBuilder).extCommunitySet = readData
    }

    override fun getBuilder(id: InstanceIdentifier<ExtCommunitySet>) = ExtCommunitySetBuilder()

    companion object {
        val VRFS_ID = InstanceIdentifier.create(Vrfs::class.java)
        val EMPTY_VRFS = VrfsBuilder().build()!!

        val ROUTE_TARGET_EXPORT_SET = "-route-target-export-set"
        val ROUTE_TARGET_IMPORT_SET = "-route-target-import-set"

        fun parseAllIds(or: Vrfs): List<ExtCommunitySetKey> {
            return or
                    .vrf.orEmpty()
                    .flatMap { it.toCommunitySets() }
                    .map { ExtCommunitySetKey(it.extCommunitySetName) }
        }

        fun parseCurrentAttributes(vrfs: Vrfs, builder: ExtCommunitySetBuilder, setName: String) {
            vrfs
                    .vrf.orEmpty()
                    .flatMap { it.toCommunitySets() }
                    .find { it.extCommunitySetName == setName }
                    ?.let {
                        builder.extCommunitySetName = it.extCommunitySetName
                        builder.setConfig(it.config)
                    }
        }
    }
}

private fun Vrf.toCommunitySets(): List<ExtCommunitySet> {
    val all = mutableListOf<ExtCommunitySet>()

    val ipv4Af = afs?.af.orEmpty().find { it.afName == VrfAddressFamily.Ipv4 }

    ipv4Af?.getAugmentation(Af1::class.java)?.bgp
            ?.exportRouteTargets
            ?.toExtCommunitySet(vrfName.value, ROUTE_TARGET_EXPORT_SET)
            ?.let { all.add(it) }

    ipv4Af?.getAugmentation(Af1::class.java)?.bgp
            ?.importRouteTargets
            ?.toExtCommunitySet(vrfName.value, ROUTE_TARGET_IMPORT_SET)
            ?.let { all.add(it) }

    return all

    // TODO Only ipv4 supported
    //    val ipv6Af = afs?.af.orEmpty().find { it.afName == VrfAddressFamily.Ipv6 }
}

private fun ROUTETARGETTABLE.toExtCommunitySet(vrfName: String, suffix: String): ExtCommunitySet? {
    val setName = vrfName + suffix

    val targets = routeTargets?.routeTarget.orEmpty()
            // TODO only AS or FourByteAs is supported, not IP
            .filter { it.type == BgpVrfRouteTarget.As || it.type == BgpVrfRouteTarget.FourByteAs }
            .flatMap { it.asOrFourByteAs.orEmpty() }
            .map { "${it.`as`}:${it.asIndex.value}" }
            .toList()

    if (targets.isEmpty()) {
        return null
    }

    return ExtCommunitySetBuilder()
            .setExtCommunitySetName(setName)
            .setConfig(ConfigBuilder()
                    .setExtCommunitySetName(setName)
                    .setExtCommunityMember(
                            targets.map { ExtCommunitySetConfig.ExtCommunityMember(BgpExtCommunityType(it)) }
                    )
                    .build())
            .build()
}