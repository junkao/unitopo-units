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

package io.frinx.unitopo.unit.xr6.routing.policy

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NoopWriter
import io.frinx.unitopo.unit.xr6.routing.policy.handlers.ExtCommunitySetConfigWriter
import io.frinx.unitopo.unit.xr6.routing.policy.handlers.ExtCommunitySetReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.DefinedSets2
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.DefinedSets2Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ExtCommunitySets
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ExtCommunitySetsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ExtCommunitySet
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.routing.policy.defined.sets.BgpDefinedSets
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.routing.policy.defined.sets.BgpDefinedSetsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.`$YangModuleInfoImpl`
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.defined.sets.top.DefinedSets
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.defined.sets.top.DefinedSetsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.routing.policy.top.RoutingPolicy
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.routing.policy.top.RoutingPolicyBuilder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.`$YangModuleInfoImpl` as UnderlayIpv4BgpConfigYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ext.community.set.Config as ExtCommunityConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.`$YangModuleInfoImpl` as OpenconfigBGPYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.afi.safi.Config as AfiSafiConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.AfiSafisBuilder as NeighborAfiSafisBuilder

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        if (reg != null) {
            reg!!.close()
        }
    }

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(
                `$YangModuleInfoImpl`.getInstance(),
                org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.`$YangModuleInfoImpl`
                        .getInstance())

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> =
            setOf(UnderlayIpv4BgpConfigYangModule.getInstance())

    override fun getRpcs(context: UnderlayAccess) = emptySet<RpcService<out DataObject, out DataObject>>()

    override fun provideHandlers(rRegistry: ModifiableReaderRegistryBuilder, wRegistry: ModifiableWriterRegistryBuilder,
                                 access: UnderlayAccess) {
        provideReaders(rRegistry, access)
        provideWriters(wRegistry, access)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, access: UnderlayAccess) {
        // provide writers
        wRegistry.add(GenericWriter<RoutingPolicy>(io.frinx.openconfig.openconfig.policy.IIDs.ROUTINGPOLICY, NoopWriter()))
        wRegistry.add(GenericWriter<DefinedSets>(io.frinx.openconfig.openconfig.policy.IIDs.RO_DEFINEDSETS, NoopWriter()))
        wRegistry.add(GenericWriter<DefinedSets2>(DEFINED_SETS_1, NoopWriter()))
        wRegistry.add(GenericWriter<BgpDefinedSets>(BGP_DEFINED_SETS, NoopWriter()))
        wRegistry.add(GenericWriter<ExtCommunitySets>(EXT_COMMUNITY_SETS, NoopWriter()))
        wRegistry.add(GenericWriter<ExtCommunitySet>(EXT_COMMUNITY_SET, NoopWriter()))
        wRegistry.addAfter(GenericWriter<org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ext.community.set.Config>(EXT_CS_CONFIG,
                ExtCommunitySetConfigWriter(access)), IIDs.NE_NE_CONFIG)
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, access: UnderlayAccess) {
        // provide readers
        rRegistry.addStructuralReader(io.frinx.openconfig.openconfig.policy.IIDs.ROUTINGPOLICY, RoutingPolicyBuilder::class.java)
        rRegistry.addStructuralReader(io.frinx.openconfig.openconfig.policy.IIDs.RO_DEFINEDSETS, DefinedSetsBuilder::class.java)
        rRegistry.addStructuralReader(DEFINED_SETS_1, DefinedSets2Builder::class.java)
        rRegistry.addStructuralReader(BGP_DEFINED_SETS, BgpDefinedSetsBuilder::class.java)
        rRegistry.addStructuralReader(EXT_COMMUNITY_SETS, ExtCommunitySetsBuilder::class.java)
        rRegistry.add(GenericConfigListReader(EXT_COMMUNITY_SET, ExtCommunitySetReader(access)))
    }

    override fun toString() = "XR 6 (2015-07-30) routing policy translate unit"

    companion object {
        private val DEFINED_SETS_1 = io.frinx.openconfig.openconfig.policy.IIDs.RO_DEFINEDSETS.augmentation(DefinedSets2::class.java)
        private val BGP_DEFINED_SETS = DEFINED_SETS_1.child(BgpDefinedSets::class.java)
        private val EXT_COMMUNITY_SETS = BGP_DEFINED_SETS.child(ExtCommunitySets::class.java)
        private val EXT_COMMUNITY_SET = EXT_COMMUNITY_SETS.child(ExtCommunitySet::class.java)
        private val EXT_CS_CONFIG = EXT_COMMUNITY_SET.child(org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ext.community.set.Config::class.java)
    }
}
