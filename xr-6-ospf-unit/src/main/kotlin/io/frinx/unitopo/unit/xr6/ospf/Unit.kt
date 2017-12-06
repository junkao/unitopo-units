/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.frinx.unitopo.unit.xr6.ospf

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
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import io.frinx.unitopo.unit.xr6.ospf.handler.*
import io.frinx.unitopo.unit.xr6.ospf.handler.AreaInterfaceWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.Ospfv2Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.AreasBuilder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.`$YangModuleInfoImpl` as UnderlayOspfConfigYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.types.rev170228.`$YangModuleInfoImpl` as OpenconfigOspfYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.cisco.rev171124.`$YangModuleInfoImpl` as OpenconfigCiscoOspfExtensionModule

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

    override fun getYangSchemas() = setOf(OpenconfigOspfYangModule.getInstance(),
            OpenconfigCiscoOspfExtensionModule.getInstance())

    override fun getUnderlayYangSchemas() = setOf(UnderlayOspfConfigYangModule.getInstance())

    override fun getRpcs(context: UnderlayAccess) = emptySet<RpcService<out DataObject, out DataObject>>()

    override fun provideHandlers(rRegistry: ModifiableReaderRegistryBuilder, wRegistry: ModifiableWriterRegistryBuilder,
                                 access: UnderlayAccess) {
        provideReaders(rRegistry, access)
        provideWriters(wRegistry, access)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, access: UnderlayAccess) {
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_GL_CONFIG, GlobalConfigWriter(access)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_GL_TIMERS, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_GL_TI_MAXMETRIC, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_GL_TI_MA_CONFIG, MaxMetricConfigWriter(access)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_AREAS, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.NE_NE_PR_PR_OS_AR_AREA, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_AR_AR_CONFIG, AreaConfigWriter(access)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_AR_AR_INTERFACES, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_INTERFACE, AreaInterfaceWriter(access)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_CONFIG, NoopWriter()))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, access: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_OSPFV2, Ospfv2Builder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_OS_GLOBAL, GlobalBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_OS_GL_CONFIG, GlobalConfigReader(access)))
        rRegistry.add(GenericOperReader(IIDs.NE_NE_PR_PR_OS_GL_STATE, GlobalStateReader(access)))
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_OS_AREAS, AreasBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PR_OS_AR_AREA, OspfAreaReader(access)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_OS_AR_AR_CONFIG, AreaConfigReader()))
        rRegistry.add(GenericOperReader(IIDs.NE_NE_PR_PR_OS_AR_AR_STATE, AreaStateReader()))
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_OS_AR_AR_INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_INTERFACE, AreaInterfaceReader(access)))
    }

    override fun toString(): String = "XR 6 (2015-07-30) OSPF translate unit"
}
