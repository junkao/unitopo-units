/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos17.mpls

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.`$YangModuleInfoImpl` as MplsYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.mpls.top.MplsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.mpls.top.mpls.TeInterfaceAttributesBuilder
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.`$YangModuleInfoImpl` as JunosYangInfo
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.mpls.handler.*
import io.frinx.unitopo.unit.network.instance.NoopListWriter
import io.frinx.unitopo.unit.network.instance.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.mpls.top.mpls.LspsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.mpls.top.mpls.lsps.ConstrainedPathBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnel.p2p_top.P2pTunnelAttributesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.TunnelsBuilder

class MplsUnit(private val registry: TranslationUnitCollector) : TranslateUnit {

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(
        MplsYangInfo.getInstance()
    )

    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit( this)
    }

    fun close() {
        if (reg != null) {
            reg!!.close()
        }
    }

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
            JunosYangInfo.getInstance()
    )

    override fun getRpcs(underlayAccess: UnderlayAccess): Set<RpcService<*, *>> = emptySet()

    override fun provideHandlers(rRegistry: ModifiableReaderRegistryBuilder,
                                 wRegistry: ModifiableWriterRegistryBuilder,
                                 underlayAccess: UnderlayAccess) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.add(GenericWriter(IIDs.NE_NE_MPLS, NoopWriter()))

        // TE
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_TE_INTERFACE, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_TE_IN_CONFIG, TeInterfaceConfigWriter(underlayAccess)))

        // Tunnel
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_LSPS, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_LS_CONSTRAINEDPATH, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_LS_CO_TU_TUNNEL, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_LS_CO_TU_TU_CONFIG, TunnelConfigWriter(underlayAccess)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_LS_CO_TU_TU_P2PTUNNELATTRIBUTES, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_LS_CO_TU_TU_P2_CONFIG, NoopWriter()))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.NE_NE_MPLS, MplsBuilder::class.java)

        // TE
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_TEINTERFACEATTRIBUTES, TeInterfaceAttributesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_MP_TE_INTERFACE, TeInterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_MP_TE_IN_CONFIG, TeInterfaceConfigReader()))

        // Tunnel
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_LSPS, LspsBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_LS_CONSTRAINEDPATH, ConstrainedPathBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_LS_CO_TUNNELS, TunnelsBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_MP_LS_CO_TU_TUNNEL, TunnelReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_MP_LS_CO_TU_TU_CONFIG, TunnelConfigReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_LS_CO_TU_TU_P2PTUNNELATTRIBUTES, P2pTunnelAttributesBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_MP_LS_CO_TU_TU_P2_CONFIG, P2pAttributesConfigReader(underlayAccess)))
    }

    override fun toString(): String {
        return "Junos 17.3 MPLS translate unit"
    }
}
