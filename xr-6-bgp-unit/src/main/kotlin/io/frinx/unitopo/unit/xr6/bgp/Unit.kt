/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp

import com.google.common.collect.Sets
import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
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
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.*
import io.frinx.unitopo.unit.xr6.vrf.AfiSafilWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.DefinedSets2
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ExtCommunitySets
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ExtCommunitySet
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.routing.policy.defined.sets.BgpDefinedSets
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.AfiSafisBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.afi.safi.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.afi.safi.StateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.TransportBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.BgpBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.NeighborsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.apply.policy.group.ApplyPolicyBuilder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
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
//        wRegistry.addAfter(GenericWriter<Config>(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, GlobalConfigWriter(cli)),
//                IIDs.NE_NE_CONFIG)
//
//        wRegistry.add(GenericWriter<AfiSafi>(IIDs.NE_NE_PR_PR_BG_GL_AF_AFISAFI, NoopCliWriter<AfiSafi>()))
//        wRegistry.addAfter(GenericWriter<Config>(IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, GlobalAfiSafiConfigWriter(cli)),
//                IIDs.NE_NE_PR_PR_BG_GL_CONFIG)
//
//        // Neighbor writer, handle also subtrees
//        wRegistry.subtreeAddAfter(
//                Sets.newHashSet<InstanceIdentifier<*>>(
//                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_CONFIG, InstanceIdentifier.create(Neighbor::class.java)),
//                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_TRANSPORT, InstanceIdentifier.create(Neighbor::class.java)),
//                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_TR_CONFIG, InstanceIdentifier.create(Neighbor::class.java)),
//                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_APPLYPOLICY, InstanceIdentifier.create(Neighbor::class.java)),
//                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_AP_CONFIG, InstanceIdentifier.create(Neighbor::class.java)),
//                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_AFISAFIS, InstanceIdentifier.create(Neighbor::class.java)),
//                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AFISAFI, InstanceIdentifier.create(Neighbor::class.java)),
//                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_CONFIG, InstanceIdentifier.create(Neighbor::class.java))),
//                GenericListWriter<Neighbor, NeighborKey>(IIDs.NE_NE_PR_PR_BG_NE_NEIGHBOR, NeighborWriter(cli)),
//                Sets.newHashSet(IIDs.NE_NE_CONFIG, IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG))

        wRegistry.add(GenericWriter(io.frinx.openconfig.openconfig.policy.IIDs.RO_DEFINEDSETS
                .augmentation(DefinedSets2::class.java)
                .child(BgpDefinedSets::class.java)
                .child(ExtCommunitySets::class.java)
                .child(ExtCommunitySet::class.java)
                .child(ExtCommunityConfig::class.java), ExtCommunitySetConfigWriter(access)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_LO_AG_CONFIG, AggregateConfigWriter(access)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BGP, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BG_GLOBAL, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, GlobalConfigWriter(access)))
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
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_GL_AFISAFIS, AfiSafisBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PR_BG_GL_AF_AFISAFI, GlobalAfiSafiReader(access)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, GlobalAfiSafiConfigReader(access)))

        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_NEIGHBORS, NeighborsBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PR_BG_NE_NEIGHBOR, NeighborReader(access)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_BG_NE_NE_CONFIG, NeighborConfigReader(access)))
        rRegistry.add(GenericOperReader(IIDs.NE_NE_PR_PR_BG_NE_NE_STATE, NeighborStateReader(access)))
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_NE_NE_AFISAFIS, NeighborAfiSafisBuilder::class.java)
        rRegistry.subtreeAdd(Sets.newHashSet<InstanceIdentifier<*>>(InstanceIdentifier.create(AfiSafi::class.java).child(Config::class.java)),
                GenericConfigListReader(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AFISAFI, NeighborAfiSafiReader(access)))

        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_NE_NE_TRANSPORT, TransportBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_BG_NE_NE_TR_CONFIG, NeighborTransportConfigReader(access)))

        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_NE_NE_APPLYPOLICY, ApplyPolicyBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_BG_NE_NE_AP_CONFIG, NeighborPolicyConfigReader(access)))

        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_STATE, StateBuilder::class.java)
        rRegistry.add(GenericOperReader(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_ST_PREFIXES, PrefixesReader(access)))
    }

    override fun toString() = "XR 6 (2015-07-30) BGP translate unit"
}

typealias IID<T> = org.opendaylight.yangtools.yang.binding.InstanceIdentifier<T>

// CFG
typealias UnderlayVrfNeighbor = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighbor
typealias UnderlayNeighbor = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.Neighbor
// OPER
typealias UnderlayOperNeighbor = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.neighbor.Neighbor
typealias UnderlayOperBgpInstance = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.bgp.instances.Instance
typealias UnderlayOperBgpInstanceKey = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.bgp.instances.InstanceKey
typealias UnderlayOperNeighborKey = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.neighbor.NeighborKey
typealias UnderlayOperNeighbors = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.neighbor.table.Neighbors
