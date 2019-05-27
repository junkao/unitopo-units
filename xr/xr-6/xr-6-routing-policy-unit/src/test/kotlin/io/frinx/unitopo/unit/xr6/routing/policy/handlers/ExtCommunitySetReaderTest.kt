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

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.hamcrest.collection.IsIterableContainingInAnyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.Vrfs
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ExtCommunitySetConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ExtCommunitySetBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ExtCommunitySetKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.BgpExtCommunityType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.`$YangModuleInfoImpl` as UnderlayIpv4BgpConfigYangModule
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayInfraRsConfigYangModule
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.YangModuleInfo

class ExtCommunitySetReaderTest : AbstractNetconfHandlerTest() {

    override fun getModels(): Collection<YangModuleInfo> {
        return setOf(
            UnderlayIpv4BgpConfigYangModule.getInstance(),
            UnderlayInfraRsConfigYangModule.getInstance()
            )
    }

    private val DATA_NODES = getResourceAsString("/vrf-conf.xml")

    @Test
    fun testAllIds() {

        val vrfs = parseGetCfgResponse(DATA_NODES, InstanceIdentifier.create(Vrfs::class.java))

        assertEquals(listOf("abcd-route-target-export-set", "abcd-route-target-import-set",
            "abcd3-route-target-import-set")
                .map { ExtCommunitySetKey(it) },

                ExtCommunitySetReader.parseAllIds(vrfs))
    }

    @Test
    fun testRead() {
        val vrfs = parseGetCfgResponse(DATA_NODES, InstanceIdentifier.create(Vrfs::class.java))

        var builder = ExtCommunitySetBuilder()
        ExtCommunitySetReader.parseCurrentAttributes(vrfs, builder, "abcd-route-target-export-set")
        var actual = builder.build()
        assertEquals("abcd-route-target-export-set", actual.extCommunitySetName)
        assertEquals("abcd-route-target-export-set", actual.config.extCommunitySetName)
        assertThat(actual.config.extCommunityMember,
                IsIterableContainingInAnyOrder.containsInAnyOrder(
                        ExtCommunitySetConfig.ExtCommunityMember(BgpExtCommunityType("8585:4343")),
                        ExtCommunitySetConfig.ExtCommunityMember(BgpExtCommunityType("1:1"))))

        builder = ExtCommunitySetBuilder()
        ExtCommunitySetReader.parseCurrentAttributes(vrfs, builder, "abcd-route-target-import-set")
        actual = builder.build()
        assertEquals("abcd-route-target-import-set", actual.extCommunitySetName)
        assertEquals("abcd-route-target-import-set", actual.config.extCommunitySetName)
        assertThat(actual.config.extCommunityMember,
                IsIterableContainingInAnyOrder.containsInAnyOrder(
                        ExtCommunitySetConfig.ExtCommunityMember(BgpExtCommunityType("6500:4")),
                        ExtCommunitySetConfig.ExtCommunityMember(BgpExtCommunityType("5445444:1"))))

        builder = ExtCommunitySetBuilder()
        ExtCommunitySetReader.parseCurrentAttributes(vrfs, builder, "NONEXISTING")
        assertEquals(ExtCommunitySetBuilder().build(), builder.build())
    }
}