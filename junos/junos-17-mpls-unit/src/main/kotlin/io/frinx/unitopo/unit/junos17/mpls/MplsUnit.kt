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
import io.frinx.unitopo.unit.junos17.mpls.handler.TeInterfaceConfigReader
import io.frinx.unitopo.unit.junos17.mpls.handler.TeInterfaceConfigWriter
import io.frinx.unitopo.unit.junos17.mpls.handler.TeInterfaceReader
import io.frinx.unitopo.unit.network.instance.NoopListWriter
import io.frinx.unitopo.unit.network.instance.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.mpls.top.Mpls
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te._interface.attributes.top.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te._interface.attributes.top.InterfaceKey

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
        wRegistry.add(GenericWriter<Mpls>(IIDs.NE_NE_MPLS, NoopWriter<Mpls>()))

        // TE
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_TE_INTERFACE, NoopListWriter<Interface, InterfaceKey>()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_TE_IN_CONFIG, TeInterfaceConfigWriter(underlayAccess)))

    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.NE_NE_MPLS, MplsBuilder::class.java)

        // TE
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_TEINTERFACEATTRIBUTES, TeInterfaceAttributesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_MP_TE_INTERFACE, TeInterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_MP_TE_IN_CONFIG, TeInterfaceConfigReader()))
    }

    override fun toString(): String {
        return "Junos 17.3 MPLS translate unit"
    }
}
