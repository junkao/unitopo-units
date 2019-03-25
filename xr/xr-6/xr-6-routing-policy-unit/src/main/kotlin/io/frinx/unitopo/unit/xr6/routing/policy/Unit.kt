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
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.policy.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NoopWriter
import io.frinx.unitopo.unit.xr6.init.Unit
import io.frinx.unitopo.unit.xr6.routing.policy.handlers.ExtCommunitySetConfigWriter
import io.frinx.unitopo.unit.xr6.routing.policy.handlers.ExtCommunitySetReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.DefinedSets2
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.DefinedSets2Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ExtCommunitySets
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ExtCommunitySetsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ExtCommunitySet
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ext.community.set.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.routing.policy.defined.sets.BgpDefinedSets
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.routing.policy.defined.sets.BgpDefinedSetsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.`$YangModuleInfoImpl`
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.defined.sets.top.DefinedSets
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.defined.sets.top.DefinedSetsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.routing.policy.top.RoutingPolicy
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.routing.policy.top.RoutingPolicyBuilder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import io.frinx.openconfig.openconfig.bgp.IIDs as BgpIIDs
import io.frinx.openconfig.openconfig.network.instance.IIDs as NeIIDs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.`$YangModuleInfoImpl` as UnderlayIpv4BgpConfigYangModule

class Unit(private val registry: TranslationUnitCollector) : Unit() {
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

    override fun provideHandlers(
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        access: UnderlayAccess
    ) {
        provideReaders(rRegistry, access)
        provideWriters(wRegistry, access)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, access: UnderlayAccess) {
        // provide writers
        wRegistry.add(GenericWriter<RoutingPolicy>(IIDs.ROUTINGPOLICY, NoopWriter()))
        wRegistry.add(GenericWriter<DefinedSets>(IIDs.RO_DEFINEDSETS, NoopWriter()))
        wRegistry.add(GenericWriter<DefinedSets2>(BgpIIDs.RO_DE_AUG_DEFINEDSETS2, NoopWriter()))
        wRegistry.add(GenericWriter<BgpDefinedSets>(BgpIIDs.RO_DE_AUG_DEFINEDSETS2_BGPDEFINEDSETS, NoopWriter()))
        wRegistry.add(GenericWriter<ExtCommunitySets>(BgpIIDs.RO_DE_AUG_DEFINEDSETS2_BG_EXTCOMMUNITYSETS, NoopWriter()))
        wRegistry.add(GenericWriter<ExtCommunitySet>(BgpIIDs.RO_DE_AUG_DEFINEDSETS2_BG_EX_EXTCOMMUNITYSET,
            NoopWriter()))
        wRegistry.addAfter(GenericWriter<Config>(BgpIIDs.RO_DE_AUG_DEFINEDSETS2_BG_EX_EX_CONFIG,
            ExtCommunitySetConfigWriter(access)), NeIIDs.NE_NE_CONFIG)
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, access: UnderlayAccess) {
        // provide readers
        rRegistry.addStructuralReader(IIDs.ROUTINGPOLICY, RoutingPolicyBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.RO_DEFINEDSETS, DefinedSetsBuilder::class.java)
        rRegistry.addStructuralReader(BgpIIDs.RO_DE_AUG_DEFINEDSETS2, DefinedSets2Builder::class.java)
        rRegistry.addStructuralReader(BgpIIDs.RO_DE_AUG_DEFINEDSETS2_BGPDEFINEDSETS, BgpDefinedSetsBuilder::class.java)
        rRegistry.addStructuralReader(BgpIIDs.RO_DE_AUG_DEFINEDSETS2_BG_EXTCOMMUNITYSETS,
            ExtCommunitySetsBuilder::class.java)
        rRegistry.add(GenericConfigListReader(BgpIIDs.RO_DE_AUG_DEFINEDSETS2_BG_EX_EXTCOMMUNITYSET,
            ExtCommunitySetReader(access)))
    }

    override fun toString() = "XR 6 (2015-07-30) routing policy translate unit"
}