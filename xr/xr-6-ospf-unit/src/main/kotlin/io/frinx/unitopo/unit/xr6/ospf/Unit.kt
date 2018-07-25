/*
 * Copyright © 2018 Frinx and others.
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
import io.frinx.unitopo.unit.xr6.ospf.handler.AreaConfigReader
import io.frinx.unitopo.unit.xr6.ospf.handler.AreaConfigWriter
import io.frinx.unitopo.unit.xr6.ospf.handler.AreaInterfaceConfigReader
import io.frinx.unitopo.unit.xr6.ospf.handler.AreaInterfaceConfigWriter
import io.frinx.unitopo.unit.xr6.ospf.handler.AreaInterfaceReader
import io.frinx.unitopo.unit.xr6.ospf.handler.AreaStateReader
import io.frinx.unitopo.unit.xr6.ospf.handler.GlobalConfigReader
import io.frinx.unitopo.unit.xr6.ospf.handler.GlobalConfigWriter
import io.frinx.unitopo.unit.xr6.ospf.handler.GlobalStateReader
import io.frinx.unitopo.unit.xr6.ospf.handler.MaxMetricConfigWriter
import io.frinx.unitopo.unit.xr6.ospf.handler.OspfAreaReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.Ospfv2Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.AreasBuilder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.`$YangModuleInfoImpl` as UnderlayOspfConfigYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.cisco.rev171124.`$YangModuleInfoImpl` as OpenconfigCiscoOspfExtensionModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.types.rev170228.`$YangModuleInfoImpl` as OpenconfigOspfYangModule

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

    override fun provideHandlers(
        rRegistry: ModifiableReaderRegistryBuilder,
        wRegistry: ModifiableWriterRegistryBuilder,
        access: UnderlayAccess
    ) {
        provideReaders(rRegistry, access)
        provideWriters(wRegistry, access)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, access: UnderlayAccess) {
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OSPFV2, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_GLOBAL, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_PR_PR_OS_GL_CONFIG, GlobalConfigWriter(access)),
                IIDs.NE_NE_PR_PR_CONFIG)

        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_GL_TIMERS, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_GL_TI_MAXMETRIC, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_GL_TI_MA_CONFIG, MaxMetricConfigWriter(access)))

        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_AREAS, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.NE_NE_PR_PR_OS_AR_AREA, NoopListWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_PR_PR_OS_AR_AR_CONFIG, AreaConfigWriter(access)),
                IIDs.NE_NE_PR_PR_OS_GL_CONFIG)
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_AR_AR_INTERFACES, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_INTERFACE, NoopListWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_CONFIG, AreaInterfaceConfigWriter(access)),
                setOf(IIDs.NE_NE_PR_PR_OS_AR_AR_CONFIG, IIDs.NE_NE_IN_IN_CONFIG, IIDs.NE_NE_PR_PR_OS_GL_CONFIG))

        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_INTERFACEREF, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_IN_CONFIG, NoopWriter()))
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
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_CONFIG, AreaInterfaceConfigReader(access)))
    }

    override fun toString(): String = "XR 6 (2015-07-30) OSPF translate unit"
}