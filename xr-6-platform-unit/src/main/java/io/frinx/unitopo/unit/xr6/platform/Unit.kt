/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.frinx.unitopo.unit.xr6.platform

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericListReader
import io.fd.honeycomb.translate.impl.read.GenericOperListReader
import io.fd.honeycomb.translate.impl.read.GenericOperReader
import io.fd.honeycomb.translate.impl.read.GenericReader
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.platform.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess

import io.frinx.unitopo.unit.xr6.platform.handler.ComponentConfigReader
import io.frinx.unitopo.unit.xr6.platform.handler.ComponentReader
import io.frinx.unitopo.unit.xr6.platform.handler.ComponentStateReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.ComponentsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.sc.invmgr.admin.oper.rev151109.`$YangModuleInfoImpl` as UnderlayInventoryModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.`$YangModuleInfoImpl` as OpenconfigPlatformModule



class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    // Invoked by blueprint
    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    // Invoked by blueprint
    fun close() {
        if (reg != null) {
            reg!!.close()
        }
    }

    override fun getYangSchemas() = setOf(OpenconfigPlatformModule.getInstance())

    override fun getUnderlayYangSchemas() = setOf(UnderlayInventoryModule.getInstance())

    override fun getRpcs(context: UnderlayAccess) = emptySet<RpcService<*, *>>()

    override fun provideHandlers(rRegistry: ModifiableReaderRegistryBuilder, wRegistry: ModifiableWriterRegistryBuilder,
                                 access: UnderlayAccess) {
        provideReaders(rRegistry, access)
        provideWriters(wRegistry, access)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, access: UnderlayAccess) {
        // no-op
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, access: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.COMPONENTS, ComponentsBuilder::class.java)
        rRegistry.add(GenericOperListReader(IIDs.CO_COMPONENT, ComponentReader(access)))
        rRegistry.add(GenericOperReader(IIDs.CO_CO_CONFIG, ComponentConfigReader()))
        rRegistry.add(GenericOperReader(IIDs.CO_CO_STATE, ComponentStateReader(access)))
    }

    override fun toString() = "xr6-platform-unit"

}
