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

package io.frinx.unitopo.unit.junos.ospf

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.ospf.handler.OspfAreaConfigWriter
import io.frinx.unitopo.unit.junos.ospf.handler.OspfAreaInterfaceConfigReader
import io.frinx.unitopo.unit.junos.ospf.handler.OspfAreaInterfaceConfigWriter
import io.frinx.unitopo.unit.junos.ospf.handler.OspfAreaInterfaceReader
import io.frinx.unitopo.unit.junos.ospf.handler.OspfAreaInterfaceWriter
import io.frinx.unitopo.unit.junos.ospf.handler.OspfAreaReader
import io.frinx.unitopo.unit.junos.ospf.handler.OspfMaxMetricConfigReader
import io.frinx.unitopo.unit.junos.ospf.handler.OspfMaxMetricConfigWriter
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.TimersBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.timers.MaxMetricBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.Ospfv2Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.AreasBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.`$YangModuleInfoImpl` as OspfYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.types.rev170228.`$YangModuleInfoImpl` as OspfTypesYangInfo
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.`$YangModuleInfoImpl` as IetfYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas() = setOf(
            OspfYangInfo.getInstance(),
            IetfYangInfo.getInstance(),
            OspfTypesYangInfo.getInstance())

    override fun getUnderlayYangSchemas() = setOf(
            UnderlayInterfacesYangInfo.getInstance())

    override fun getRpcs(underlayAccess: UnderlayAccess) = emptySet<RpcService<*, *>>()

    override fun provideHandlers(
        rRegistry: ModifiableReaderRegistryBuilder,
        wRegistry: ModifiableWriterRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OSPFV2, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_GLOBAL, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_GL_TIMERS, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_GL_TI_MAXMETRIC, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_GL_TI_MA_CONFIG, OspfMaxMetricConfigWriter(underlayAccess)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_AREAS, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.NE_NE_PR_PR_OS_AR_AREA, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_AR_AR_CONFIG, OspfAreaConfigWriter(underlayAccess)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_AR_AR_INTERFACES, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_INTERFACE,
            OspfAreaInterfaceWriter(underlayAccess)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_CONFIG,
            OspfAreaInterfaceConfigWriter(underlayAccess)))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_OSPFV2, Ospfv2Builder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_OS_GLOBAL, GlobalBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_OS_GL_TIMERS, TimersBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_OS_GL_TI_MAXMETRIC, MaxMetricBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_OS_GL_TI_MA_CONFIG,
            OspfMaxMetricConfigReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_OS_AREAS, AreasBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PR_OS_AR_AREA, OspfAreaReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_OS_AR_AR_INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_INTERFACE,
            OspfAreaInterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_CONFIG,
            OspfAreaInterfaceConfigReader(underlayAccess)))
    }

    override fun toString(): String = "Junos 17.3 ospf translate unit"
}