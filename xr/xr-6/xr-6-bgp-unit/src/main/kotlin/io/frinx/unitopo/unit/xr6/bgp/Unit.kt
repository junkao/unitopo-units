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

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalAfiSafiConfigReader
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalAfiSafiConfigWriter
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalAfiSafiReader
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalConfigReader
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalConfigWriter
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalStateReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborEbgpMultihopConfigReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.PrefixesReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.afisafi.ApplyPolicyConfigReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborTransportConfigReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborWriter
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborConfigReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.NeighborStateReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.afisafi.AfiSafiReader
import io.frinx.unitopo.unit.xr6.bgp.handler.neighbor.afisafi.Ipv6UnicastReader
import io.frinx.unitopo.unit.xr6.bgp.handler.peergroup.PeerGroupAfiSafiApplyPolicyConfigReader
import io.frinx.unitopo.unit.xr6.bgp.handler.peergroup.PeerGroupAfiSafiListReader
import io.frinx.unitopo.unit.xr6.bgp.handler.peergroup.PeerGroupConfigWriter
import io.frinx.unitopo.unit.xr6.bgp.handler.peergroup.PeerGroupListReader
import io.frinx.unitopo.unit.xr6.bgp.handler.peergroup.PeerGroupAfiSafiConfigWriter
import io.frinx.unitopo.unit.xr6.bgp.handler.peergroup.PeerGroupAfiSafiApplyPolicyConfigWriter
import io.frinx.unitopo.unit.xr6.init.Unit
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.afi.safi.Config
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.`$YangModuleInfoImpl` as UnderlayIpv4BgpConfigYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.`$YangModuleInfoImpl` as OpenconfigBGPYangModule

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

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(OpenconfigBGPYangModule.getInstance(),
            IIDs.FRINX_BGP_EXTENSION)

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
        wRegistry.addAfter(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, GlobalConfigWriter(access),
                IIDs.NE_NE_CONFIG)

        wRegistry.addNoop(IIDs.NE_NE_PR_PR_BG_GL_AF_AFISAFI)
        wRegistry.addAfter(IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, GlobalAfiSafiConfigWriter(access),
                IIDs.NE_NE_PR_PR_BG_GL_CONFIG)

        wRegistry.subtreeAddAfter(IIDs.NE_NE_PR_PR_BG_NE_NEIGHBOR, NeighborWriter(access),
                setOf(IIDs.NE_NE_PR_PR_BG_NE_NE_CONFIG,
                        IIDs.NE_NE_PR_PR_BG_NE_NE_TRANSPORT,
                        IIDs.NE_NE_PR_PR_BG_NE_NE_TR_CONFIG,
                        IIDs.NE_NE_PR_PR_BG_NE_NE_APPLYPOLICY,
                        IIDs.NE_NE_PR_PR_BG_NE_NE_AP_CONFIG,
                        IIDs.NE_NE_PR_PR_BG_NE_NE_AFISAFIS,
                        IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AFISAFI,
                        IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_CONFIG,
                        IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_AP_CONFIG,
                        IIDs.NE_NE_PR_PR_BG_NE_NE_EB_CONFIG,
                        IIDs.NET_NET_PRO_PRO_BGP_NEI_NEI_AFI_AFI_IPV_CONFIG,
                        IIDs.NETWO_NETWO_PROTO_PROTO_BGP_NEIGH_NEIGH_AFISA_AFISA_IPV6U_PREFI_CONFIG,
                        IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_CO_AUG_BGPNEAFAUG,
                        IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_CO_AUG_BGPNEAFAUG_SOFTRECONFIGURATION),
                IIDs.NE_NE_CONFIG, IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG)

        // peer group
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_BG_PE_PEERGROUP)
        wRegistry.addAfter(IIDs.NE_NE_PR_PR_BG_PE_PE_CONFIG, PeerGroupConfigWriter(access),
            IIDs.NE_NE_PR_PR_BG_GL_CONFIG)
        // peer group afisafi
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_BG_PE_PE_AF_AFISAFI)
        wRegistry.addAfter(IIDs.NE_NE_PR_PR_BG_PE_PE_AF_AF_CONFIG, PeerGroupAfiSafiConfigWriter(access),
            IIDs.NE_NE_PR_PR_BG_GL_CONFIG,
            IIDs.NE_NE_PR_PR_BG_PE_PE_CONFIG)
        wRegistry.addAfter(IIDs.NE_NE_PR_PR_BG_PE_PE_AF_AF_AP_CONFIG, PeerGroupAfiSafiApplyPolicyConfigWriter(access),
            IIDs.NE_NE_PR_PR_BG_GL_CONFIG,
            IIDs.NE_NE_PR_PR_BG_PE_PE_CONFIG,
            IIDs.NE_NE_PR_PR_BG_PE_PE_AF_AF_CONFIG)
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, access: UnderlayAccess) {
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, GlobalConfigReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_GL_STATE, GlobalStateReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_GL_AF_AFISAFI, GlobalAfiSafiReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, GlobalAfiSafiConfigReader(access))

        // neighbor
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_NE_NEIGHBOR, NeighborReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_NE_NE_CONFIG, NeighborConfigReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_NE_NE_STATE, NeighborStateReader(access))
        rRegistry.subtreeAdd(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AFISAFI, AfiSafiReader(access), setOf(IID_AFISAFI_CONFIG))
        rRegistry.subtreeAddAfter(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_IPV6UNICAST,
                Ipv6UnicastReader(access), setOf(IIDs.NET_NET_PRO_PRO_BGP_NEI_NEI_AFI_AFI_IPV_CONFIG),
            IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_CONFIG)
        rRegistry.addAfter(IIDs.NE_NE_PR_PR_BG_NE_NE_EB_CONFIG, NeighborEbgpMultihopConfigReader(access),
                IIDs.NE_NE_PR_PR_BG_NE_NE_CONFIG)

        rRegistry.add(IIDs.NE_NE_PR_PR_BG_NE_NE_TR_CONFIG, NeighborTransportConfigReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_AP_CONFIG, ApplyPolicyConfigReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_ST_PREFIXES, PrefixesReader(access))

        // peer-group
        rRegistry.subtreeAddAfter(IIDs.NE_NE_PR_PR_BG_PE_PEERGROUP, PeerGroupListReader(access),
            setOf(IIDs.NE_NE_PR_PR_BG_PE_PE_CONFIG),
            IIDs.NE_NE_PR_PR_BG_GL_CONFIG)
        rRegistry.subtreeAddAfter(IIDs.NE_NE_PR_PR_BG_PE_PE_AF_AFISAFI, PeerGroupAfiSafiListReader(access),
            setOf(IIDs.NE_NE_PR_PR_BG_PE_PE_AF_AF_CONFIG),
            IIDs.NE_NE_PR_PR_BG_PE_PE_CONFIG)
        rRegistry.addAfter(IIDs.NE_NE_PR_PR_BG_PE_PE_AF_AF_AP_CONFIG, PeerGroupAfiSafiApplyPolicyConfigReader(access),
            IIDs.NE_NE_PR_PR_BG_PE_PE_AF_AF_CONFIG)
    }

    companion object {
        val IID_AFISAFI_CONFIG = InstanceIdentifier.create(AfiSafi::class.java)
                .child(Config::class.java)
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
typealias UnderlayNeighborGroup = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance
.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.NeighborGroup