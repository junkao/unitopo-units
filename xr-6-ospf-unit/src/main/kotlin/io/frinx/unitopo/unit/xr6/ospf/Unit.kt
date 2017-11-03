/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.frinx.unitopo.unit.xr6.ospf

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericListReader
import io.fd.honeycomb.translate.impl.read.GenericReader
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.ospf.handler.AreaInterfaceReader
import io.frinx.unitopo.unit.xr6.ospf.handler.AreaReader
import io.frinx.unitopo.unit.xr6.ospf.handler.Ospfv2GlobalReader
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.Ospfv2Builder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.AreasBuilder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.`$YangModuleInfoImpl` as UnderlayOspfConfigYangModule
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospf.types.rev170228.`$YangModuleInfoImpl` as OpenconfigOspfYangModule

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

    override fun getYangSchemas() = setOf(OpenconfigOspfYangModule.getInstance())

    override fun getUnderlayYangSchemas() = setOf(UnderlayOspfConfigYangModule.getInstance())

    override fun getRpcs(context: UnderlayAccess) = emptySet<RpcService<out DataObject, out DataObject>>()

    override fun provideHandlers(rRegistry: ModifiableReaderRegistryBuilder, wRegistry: ModifiableWriterRegistryBuilder,
                                 access: UnderlayAccess) {
        provideReaders(rRegistry, access)
        provideWriters(wRegistry, access)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, access: UnderlayAccess) {
        // no-op
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, access: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_OSPFV2, Ospfv2Builder::class.java)

        rRegistry.subtreeAdd(
                setOf(
                        InstanceIdentifier.create(IIDs.NE_NE_PR_PR_OS_GLOBAL.targetType).child(IIDs.NE_NE_PR_PR_OS_GL_CONFIG.targetType),
                        InstanceIdentifier.create(IIDs.NE_NE_PR_PR_OS_GLOBAL.targetType).child(IIDs.NE_NE_PR_PR_OS_GL_STATE.targetType)),
                GenericReader(IIDs.NE_NE_PR_PR_OS_GLOBAL, Ospfv2GlobalReader(access)))

        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_OS_AREAS, AreasBuilder::class.java)

        rRegistry.subtreeAdd(
                setOf(
                        InstanceIdentifier.create(IIDs.NE_NE_PR_PR_OS_AR_AREA.targetType).child(IIDs.NE_NE_PR_PR_OS_AR_AR_CONFIG.targetType),
                        InstanceIdentifier.create(IIDs.NE_NE_PR_PR_OS_AR_AREA.targetType).child(IIDs.NE_NE_PR_PR_OS_AR_AR_STATE.targetType)),
                GenericListReader(IIDs.NE_NE_PR_PR_OS_AR_AREA, AreaReader(access)))

        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_OS_AR_AR_INTERFACES, InterfacesBuilder::class.java)

        rRegistry.subtreeAdd(
                setOf(
                        InstanceIdentifier.create(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_INTERFACE.targetType).child(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_CONFIG.targetType),
                        InstanceIdentifier.create(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_INTERFACE.targetType).child(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_STATE.targetType)),
                GenericListReader(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_INTERFACE, AreaInterfaceReader(access)))
    }

    override fun toString(): String = "XR 6 (2015-07-30) OSPF translate unit"
}
