/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos17.bgp

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpGlobalConfigReader
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpGlobalConfigWriter
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpNeighborConfigReader
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpNeighborConfigWriter
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpNeighborListReader
import io.frinx.unitopo.unit.utils.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.BgpBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.NeighborsBuilder
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.`$YangModuleInfoImpl` as OpenconfigBgp
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.`$YangModuleInfoImpl` as NetInstanceYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.`$YangModuleInfoImpl` as OpenconfigNetworkInstances
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.`$YangModuleInfoImpl` as JunosYangInfo

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    override fun getUnderlayYangSchemas(): MutableSet<YangModuleInfo> = setOf(
            JunosYangInfo.getInstance()
    ).toMutableSet()

    override fun getYangSchemas() = setOf(
        OpenconfigBgp.getInstance(),
        OpenconfigNetworkInstances.getInstance()
    )

    override fun provideHandlers(rRegistry: ModifiableReaderRegistryBuilder,
                                 wRegistry: ModifiableWriterRegistryBuilder,
                                 underlayAccess: UnderlayAccess) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getRpcs(underlayAccess: UnderlayAccess) = emptySet<RpcService<*, *>>()

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BGP, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BG_GLOBAL, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, BgpGlobalConfigWriter(underlayAccess)), IIDs.NE_NE_PR_PR_BG_GLOBAL)

        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BG_NEIGHBORS, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BG_NE_NEIGHBOR, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_BG_NE_NE_CONFIG, BgpNeighborConfigWriter(underlayAccess)))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BGP, BgpBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_GLOBAL, GlobalBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, BgpGlobalConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_NEIGHBORS, NeighborsBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PR_BG_NE_NEIGHBOR, BgpNeighborListReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_BG_NE_NE_CONFIG, BgpNeighborConfigReader(underlayAccess)))
    }

    override fun toString(): String = "Junos 17.3 BGP translate unit"
}


