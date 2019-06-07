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
package io.frinx.unitopo.unit.xr66.ospf

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr66.init.Unit
import io.frinx.unitopo.unit.xr66.ospf.handler.AreaConfigReader
import io.frinx.unitopo.unit.xr66.ospf.handler.AreaConfigWriter
import io.frinx.unitopo.unit.xr66.ospf.handler.AreaInterfaceConfigReader
import io.frinx.unitopo.unit.xr66.ospf.handler.AreaInterfaceConfigWriter
import io.frinx.unitopo.unit.xr66.ospf.handler.AreaInterfaceReader
import io.frinx.unitopo.unit.xr66.ospf.handler.GlobalConfigReader
import io.frinx.unitopo.unit.xr66.ospf.handler.GlobalConfigWriter
import io.frinx.unitopo.unit.xr66.ospf.handler.OspfAreaReader
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.`$YangModuleInfoImpl` as UnderlayOspfConfigYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.cisco.rev171124.`$YangModuleInfoImpl` as OpenconfigCiscoOspfExtensionModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.types.rev170228.`$YangModuleInfoImpl` as OpenconfigOspfYangModule

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

    override fun getYangSchemas() = setOf(OpenconfigOspfYangModule.getInstance(),
            OpenconfigCiscoOspfExtensionModule.getInstance())

    override fun getUnderlayYangSchemas() = setOf(UnderlayOspfConfigYangModule.getInstance())

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
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OSPFV2)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OS_GLOBAL)
        wRegistry.addAfter(IIDs.NE_NE_PR_PR_OS_GL_CONFIG, GlobalConfigWriter(access),
            IIDs.NE_NE_PR_PR_CONFIG)

        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OS_AREAS)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OS_AR_AREA)
        wRegistry.addAfter(IIDs.NE_NE_PR_PR_OS_AR_AR_CONFIG, AreaConfigWriter(access),
            IIDs.NE_NE_PR_PR_OS_GL_CONFIG)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OS_AR_AR_INTERFACES)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_INTERFACE)
        wRegistry.addAfter(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_CONFIG, AreaInterfaceConfigWriter(access),
            IIDs.NE_NE_PR_PR_OS_AR_AR_CONFIG, IIDs.NE_NE_IN_IN_CONFIG, IIDs.NE_NE_PR_PR_OS_GL_CONFIG)

        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_INTERFACEREF)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_IN_CONFIG)
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, access: UnderlayAccess) {
        rRegistry.add(IIDs.NE_NE_PR_PR_OS_GL_CONFIG, GlobalConfigReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_OS_AR_AREA, OspfAreaReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_OS_AR_AR_CONFIG, AreaConfigReader())
        rRegistry.add(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_INTERFACE, AreaInterfaceReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_CONFIG, AreaInterfaceConfigReader(access))
    }

    override fun toString(): String = "XR 6.6 (2015-07-30) OSPF translate unit"
}