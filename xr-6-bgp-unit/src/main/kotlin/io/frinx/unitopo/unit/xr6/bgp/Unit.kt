/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.read.GenericOperReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NoopWriter
import io.frinx.unitopo.unit.xr6.bgp.handler.*
import io.frinx.unitopo.unit.xr6.vrf.AfiSafilWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.DefinedSets2
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ExtCommunitySets
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ExtCommunitySet
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ext.community.set.Config as ExtCommunityConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.routing.policy.defined.sets.BgpDefinedSets
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.afi.safi.Config as AfiSafiConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.BgpBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.GlobalBuilder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.`$YangModuleInfoImpl` as UnderlayIpv4BgpConfigYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.`$YangModuleInfoImpl` as OpenconfigBGPYangModule

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

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(OpenconfigBGPYangModule.getInstance())

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> =
            setOf(UnderlayIpv4BgpConfigYangModule.getInstance())

    override fun getRpcs(context: UnderlayAccess) = emptySet<RpcService<out DataObject, out DataObject>>()

    override fun provideHandlers(rRegistry: ModifiableReaderRegistryBuilder, wRegistry: ModifiableWriterRegistryBuilder,
                                 access: UnderlayAccess) {
        provideReaders(rRegistry, access)
        provideWriters(wRegistry, access)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, access: UnderlayAccess) {
        wRegistry.add(GenericWriter(io.frinx.openconfig.openconfig.policy.IIDs.RO_DEFINEDSETS
                .augmentation(DefinedSets2::class.java)
                .child(BgpDefinedSets::class.java)
                .child(ExtCommunitySets::class.java)
                .child(ExtCommunitySet::class.java)
                .child(ExtCommunityConfig::class.java), ExtCommunitySetConfigWriter(access)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_LO_AG_CONFIG, AggregateConfigWriter(access)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BGP, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BG_GLOBAL, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BG_GL_AFISAFIS, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.NE_NE_PR_PR_BG_GL_AF_AFISAFI, AfiSafilWriter(access)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BG_NE_NE_CONFIG, NeighborConfigWriter(access)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BG_NE_NE_AP_CONFIG, NeighborApplyPolicyConfigWriter(access)))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, access: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BGP, BgpBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_GLOBAL, GlobalBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, GlobalConfigReader(access)))
        rRegistry.add(GenericOperReader(IIDs.NE_NE_PR_PR_BG_GL_STATE, GlobalStateReader(access)))
    }

    override fun toString() = "XR 6 (2015-07-30) BGP translate unit"
}
