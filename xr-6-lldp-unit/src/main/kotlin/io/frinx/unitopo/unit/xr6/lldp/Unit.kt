/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lldp

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.*
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.lldp.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp._interface.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.NeighborsBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp.top.LldpBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ethernet.lldp.cfg.rev151109.`$YangModuleInfoImpl` as UnderlayLldpCfgYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ethernet.lldp.oper.rev151109.`$YangModuleInfoImpl` as UnderlayLldpOperYangInfo
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.`$YangModuleInfoImpl` as LldpYangInfo

class Unit (private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas() = setOf(LldpYangInfo.getInstance())

    override fun getUnderlayYangSchemas() = setOf(
            UnderlayLldpCfgYangInfo.getInstance(),
            UnderlayLldpOperYangInfo.getInstance())

    override fun getRpcs(underlayAccess: UnderlayAccess) = emptySet<RpcService<*, *>>()

    override fun provideHandlers(rRegistry: ModifiableReaderRegistryBuilder,
                                 wRegistry: ModifiableWriterRegistryBuilder,
                                 underlayAccess: UnderlayAccess) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {

    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.LLDP, LldpBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.LL_INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.LL_IN_INTERFACE, InterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.LL_IN_IN_CONFIG, InterfaceConfigReader()))
        rRegistry.add(GenericOperReader(IIDs.LL_IN_IN_STATE, InterfaceStateReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.LL_IN_IN_NEIGHBORS, NeighborsBuilder::class.java)
        rRegistry.add(GenericOperListReader(IIDs.LL_IN_IN_NE_NEIGHBOR,  NeighborReader(underlayAccess)))
        rRegistry.add(GenericOperReader(IIDs.LL_IN_IN_NE_NE_STATE,  NeighborStateReader(underlayAccess)))
    }

    override fun toString(): String = "XR 6 (2015-11-09) LLDP translate unit"
}


