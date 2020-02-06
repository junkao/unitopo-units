/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.bgp

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalAfiSafiConfigReader
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalAfiSafiConfigWriter
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalAfiSafiReader
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalConfigReader
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalConfigWriter
import io.frinx.unitopo.unit.xr7.bgp.handler.neighbor.NeighborAfiSafiConfigReader
import io.frinx.unitopo.unit.xr7.bgp.handler.neighbor.NeighborAfiSafiReader
import io.frinx.unitopo.unit.xr7.bgp.handler.neighbor.NeighborConfigReader
import io.frinx.unitopo.unit.xr7.bgp.handler.neighbor.NeighborReader
import io.frinx.unitopo.unit.xr7.bgp.handler.neighbor.NeighborTransportConfigReader
import io.frinx.unitopo.unit.xr7.bgp.handler.neighbor.NeighborWriter
import io.frinx.unitopo.unit.xr7.bgp.handler.peergroup.PeerGroupAfiSafiApplyPolicyConfigReader
import io.frinx.unitopo.unit.xr7.bgp.handler.peergroup.PeerGroupListReader
import io.frinx.unitopo.unit.xr7.bgp.handler.peergroup.PeerGroupAfiSafiListReader
import io.frinx.unitopo.unit.xr7.bgp.handler.peergroup.PeerGroupConfigWriter
import io.frinx.unitopo.unit.xr7.bgp.handler.peergroup.PeerGroupAfiSafiConfigWriter
import io.frinx.unitopo.unit.xr7.bgp.handler.peergroup.PeerGroupAfiSafiApplyPolicyConfigWriter
import io.frinx.unitopo.unit.xr7.init.Unit
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.afi.safi.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.`$YangModuleInfoImpl` as UnderlayIpv4BgpConfigYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.`$YangModuleInfoImpl` as BgpYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.`$YangModuleInfoImpl` as LocalAggregatesYangModule

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
        BgpYangModule.getInstance(),
        LocalAggregatesYangModule.getInstance()
    )

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
        UnderlayIpv4BgpConfigYangModule.getInstance()
    )

    override fun getRpcs(context: UnderlayAccess) = emptySet<RpcService<out DataObject, out DataObject>>()

    override fun provideHandlers(
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, access: UnderlayAccess) {
        wRegistry.addAfter(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, GlobalConfigWriter(access), IIDs.NE_NE_CONFIG)

        wRegistry.addNoop(IIDs.NE_NE_PR_PR_BG_GL_AF_AFISAFI)

        wRegistry.addAfter(IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, GlobalAfiSafiConfigWriter(access),
            IIDs.NE_NE_PR_PR_BG_GL_CONFIG)

        wRegistry.subtreeAddAfter(IIDs.NE_NE_PR_PR_BG_NE_NEIGHBOR, NeighborWriter(access),
            setOf(
                RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_CONFIG, IID_NEIGHBOR),
                RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_CONFIG, IID_NEIGHBOR),
                RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_TRANSPORT, IID_NEIGHBOR),
                RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_TR_CONFIG, IID_NEIGHBOR),
                RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_APPLYPOLICY, IID_NEIGHBOR),
                RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_AP_CONFIG, IID_NEIGHBOR),
                RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_AFISAFIS, IID_NEIGHBOR),
                RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AFISAFI, IID_NEIGHBOR),
                RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_CONFIG, IID_NEIGHBOR)),
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
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_GL_AF_AFISAFI, GlobalAfiSafiReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, GlobalAfiSafiConfigReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_NE_NEIGHBOR, NeighborReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_NE_NE_CONFIG, NeighborConfigReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_CONFIG, NeighborAfiSafiConfigReader(access))
        rRegistry.subtreeAdd(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AFISAFI, NeighborAfiSafiReader(access),
            setOf(IID_AFISAFI_CONFIG))
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_NE_NE_TR_CONFIG, NeighborTransportConfigReader(access))

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
        val IID_NEIGHBOR = InstanceIdentifier.create(Neighbor::class.java)
        val IID_AFISAFI_CONFIG = InstanceIdentifier.create(AfiSafi::class.java)
            .child(Config::class.java)
    }

    override fun toString() = "Translate unit for Cisco-IOS-XR-ipv4-bgp-cfg@2019-04-05"
}

typealias IID<T> = org.opendaylight.yangtools.yang.binding.InstanceIdentifier<T>

typealias UnderlayDefaultVrfGlobal = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.Global

typealias UnderlayBgp = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev190405.Bgp

typealias UnderlayBgpBuilder = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev190405.BgpBuilder

// CFG
typealias UnderlayVrfNeighborBuilder = org.opendaylight.yang.gen.v1.http
.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance
.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighborBuilder

typealias UnderlayVrfNeighborKey = org.opendaylight.yang.gen.v1.http
.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance
.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighborKey

typealias UnderlayNeighbor = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance
.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.Neighbor

typealias UnderlayNeighborBuilder = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance
.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.NeighborBuilder

typealias UnderlayNeighborKey = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance
.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.NeighborKey

typealias UnderlayVrfGlobal = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfGlobal

typealias UnderlayNeighborGroup = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance
.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.NeighborGroup