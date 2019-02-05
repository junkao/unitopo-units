/*
 * Copyright © 2019 Frinx and others.
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
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalAfiSafiConfigReader
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalAfiSafiConfigWriter
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalConfigReader
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalConfigWriter
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalAfiSafiReader
import io.frinx.unitopo.unit.xr7.bgp.handler.neighbor.NeighborAfiSafiConfigReader
import io.frinx.unitopo.unit.xr7.bgp.handler.neighbor.NeighborAfiSafiReader
import io.frinx.unitopo.unit.xr7.bgp.handler.neighbor.NeighborReader
import io.frinx.unitopo.unit.xr7.bgp.handler.neighbor.NeighborWriter
import io.frinx.unitopo.unit.xr7.bgp.handler.neighbor.NeighborConfigReader
import io.frinx.unitopo.unit.xr7.bgp.handler.neighbor.NeighborTransportConfigReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.AfiSafisBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.afi.safi.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.TransportBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.BgpBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.NeighborsBuilder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.`$YangModuleInfoImpl` as UnderlayIpv4BgpConfigYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.`$YangModuleInfoImpl` as BgpYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.AfiSafisBuilder as NeighborAfiSafisBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.`$YangModuleInfoImpl` as LocalAggregatesYangModule

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
        BgpYangModule.getInstance(),
        LocalAggregatesYangModule.getInstance()
    )

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
        UnderlayIpv4BgpConfigYangModule.getInstance()
    )

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

        wRegistry.add(GenericListWriter(IIDs.NE_NE_PR_PR_BG_GL_AF_AFISAFI, NoopListWriter()))

        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, GlobalAfiSafiConfigWriter(access)),
            IIDs.NE_NE_PR_PR_BG_GL_CONFIG)

        wRegistry.subtreeAddAfter(
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
            GenericListWriter(IIDs.NE_NE_PR_PR_BG_NE_NEIGHBOR, NeighborWriter(access)),
            setOf(IIDs.NE_NE_CONFIG, IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, access: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BGP, BgpBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_GLOBAL, GlobalBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, GlobalConfigReader(access)))
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_GL_AFISAFIS, AfiSafisBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PR_BG_GL_AF_AFISAFI, GlobalAfiSafiReader(access)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, GlobalAfiSafiConfigReader(access)))
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_NEIGHBORS, NeighborsBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PR_BG_NE_NEIGHBOR, NeighborReader(access)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_BG_NE_NE_CONFIG, NeighborConfigReader(access)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_CONFIG, NeighborAfiSafiConfigReader(access)))
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_NE_NE_AFISAFIS, NeighborAfiSafisBuilder::class.java)
        rRegistry.subtreeAdd(setOf(IID_AFISAFI_CONFIG),
            GenericConfigListReader(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AFISAFI, NeighborAfiSafiReader(access)))
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_NE_NE_TRANSPORT, TransportBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_BG_NE_NE_TR_CONFIG, NeighborTransportConfigReader(access)))
    }

    companion object {
        val IID_NEIGHBOR = InstanceIdentifier.create(Neighbor::class.java)
        val IID_AFISAFI_CONFIG = InstanceIdentifier.create(AfiSafi::class.java)
            .child(Config::class.java)
    }
    override fun toString() = "Translate unit for Cisco-IOS-XR-ipv4-bgp-cfg@2019-06-15"
}

typealias IID<T> = org.opendaylight.yangtools.yang.binding.InstanceIdentifier<T>

typealias UnderlayDefaultVrfGlobal = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.Global

typealias UnderlayBgp = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev180615.Bgp

typealias UnderlayBgpBuilder = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev180615.BgpBuilder

// CFG
typealias UnderlayVrfNeighborBuilder = org.opendaylight.yang.gen.v1.http
.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance
.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighborBuilder

typealias UnderlayVrfNeighborKey = org.opendaylight.yang.gen.v1.http
.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance
.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighborKey

typealias UnderlayNeighbor = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance
.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.Neighbor

typealias UnderlayNeighborBuilder = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance
.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.NeighborBuilder

typealias UnderlayNeighborKey = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance
.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.NeighborKey

typealias UnderlayVrfGlobal = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfGlobal