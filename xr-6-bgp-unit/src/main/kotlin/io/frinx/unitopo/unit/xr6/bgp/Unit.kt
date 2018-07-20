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

package io.frinx.unitopo.unit.xr6.bgp

import com.google.common.collect.Sets
import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.read.GenericOperReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NoopWriter
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalAfiSafiConfigReader
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalAfiSafiConfigWriter
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalAfiSafiReader
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalConfigReader
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalConfigWriter
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalStateReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborAfiSafiReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborConfigReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborPolicyConfigReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborStateReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborTransportConfigReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborWriter
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.PrefixesReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.AfiSafisBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.afi.safi.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.afi.safi.StateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.TransportBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.BgpBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.NeighborsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.apply.policy.group.ApplyPolicyBuilder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.`$YangModuleInfoImpl` as UnderlayIpv4BgpConfigYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.`$YangModuleInfoImpl` as OpenconfigBGPYangModule
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

    override fun provideHandlers(
        rRegistry: ModifiableReaderRegistryBuilder,
        wRegistry: ModifiableWriterRegistryBuilder,
        access: UnderlayAccess
    ) {
        provideReaders(rRegistry, access)
        provideWriters(wRegistry, access)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, access: UnderlayAccess) {
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, GlobalConfigWriter(access)),
                IIDs.NE_NE_CONFIG)

        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BG_GL_AF_AFISAFI, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, GlobalAfiSafiConfigWriter(access)),
                IIDs.NE_NE_PR_PR_BG_GL_CONFIG)

        wRegistry.subtreeAddAfter(
                Sets.newHashSet<InstanceIdentifier<out DataObject>>(
                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_CONFIG,
                            InstanceIdentifier.create(Neighbor::class.java)),
                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_TRANSPORT,
                            InstanceIdentifier.create(Neighbor::class.java)),
                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_TR_CONFIG,
                            InstanceIdentifier.create(Neighbor::class.java)),
                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_APPLYPOLICY,
                            InstanceIdentifier.create(Neighbor::class.java)),
                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_AP_CONFIG,
                            InstanceIdentifier.create(Neighbor::class.java)),
                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_AFISAFIS,
                            InstanceIdentifier.create(Neighbor::class.java)),
                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AFISAFI,
                            InstanceIdentifier.create(Neighbor::class.java)),
                        RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_CONFIG,
                            InstanceIdentifier.create(Neighbor::class.java))),
                GenericListWriter(IIDs.NE_NE_PR_PR_BG_NE_NEIGHBOR, NeighborWriter(access)),
                Sets.newHashSet(IIDs.NE_NE_CONFIG, IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG))
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
        rRegistry.subtreeAdd(Sets.newHashSet<InstanceIdentifier<*>>(InstanceIdentifier.create(AfiSafi::class.java)
            .child(Config::class.java)),
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
typealias UnderlayBgp = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.Bgp
typealias UnderlayBgpBuilder = org.opendaylight.yang.gen.v1.http.cisco
.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.BgpBuilder
typealias UnderlayVrfNeighbor = org.opendaylight.yang.gen.v1.http.cisco
.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance
.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighbor
typealias UnderlayVrfNeighborBuilder = org.opendaylight.yang.gen.v1.http
.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance
.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighborBuilder
typealias UnderlayVrfNeighborKey = org.opendaylight.yang.gen.v1.http
.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance
.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighborKey
typealias UnderlayNeighbor = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance
.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.Neighbor
typealias UnderlayNeighborBuilder = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance
.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.NeighborBuilder
typealias UnderlayNeighborKey = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance
.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.NeighborKey
typealias UnderlayDefaultVrfGlobal = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.Global
typealias UnderlayVrfGlobal = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfGlobal
// OPER
typealias UnderlayOperNeighbor = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.oper.rev150827.neighbor.Neighbor
typealias UnderlayOperBgpInstance = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.oper.rev150827.bgp.instances.Instance
typealias UnderlayOperBgpInstanceKey = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.oper.rev150827.bgp.instances.InstanceKey
typealias UnderlayOperNeighborKey = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.oper.rev150827.neighbor.NeighborKey
typealias UnderlayOperNeighbors = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.oper.rev150827.neighbor.table.Neighbors
// Types
typealias UnderlayRouteDistinguisher = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.RouteDistinguisher
typealias UnderlayRouteDistinguisherBuilder = org.opendaylight.yang.gen.v1.http
.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance
.`as`.four._byte.`as`.vrfs.vrf.vrf.global.RouteDistinguisherBuilder