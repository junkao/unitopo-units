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
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.ospf.handler.OspfAreaConfigReader
import io.frinx.unitopo.unit.junos.ospf.handler.OspfAreaConfigWriter
import io.frinx.unitopo.unit.junos.ospf.handler.OspfAreaInterfaceConfigReader
import io.frinx.unitopo.unit.junos.ospf.handler.OspfAreaInterfaceConfigWriter
import io.frinx.unitopo.unit.junos.ospf.handler.OspfAreaInterfaceReader
import io.frinx.unitopo.unit.junos.ospf.handler.OspfAreaReader
import io.frinx.unitopo.unit.junos.ospf.handler.OspfMaxMetricConfigReader
import io.frinx.unitopo.unit.junos.ospf.handler.OspfMaxMetricConfigWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.types.rev170228.`$YangModuleInfoImpl` as OspfTypesYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.`$YangModuleInfoImpl` as OspfYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.`$YangModuleInfoImpl` as IetfYangInfo

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
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OSPFV2)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OS_GLOBAL)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OS_GL_TIMERS)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OS_GL_TI_MAXMETRIC)
        wRegistry.add(IIDs.NE_NE_PR_PR_OS_GL_TI_MA_CONFIG, OspfMaxMetricConfigWriter(underlayAccess))
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OS_AREAS)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OS_AR_AREA)
        wRegistry.add(IIDs.NE_NE_PR_PR_OS_AR_AR_CONFIG, OspfAreaConfigWriter(underlayAccess))
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OS_AR_AR_INTERFACES)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_INTERFACE)
        wRegistry.addAfter(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_CONFIG, OspfAreaInterfaceConfigWriter(underlayAccess),
            IIDs.NE_NE_PR_PR_OS_AR_AR_CONFIG)
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.add(IIDs.NE_NE_PR_PR_OS_GL_TI_MA_CONFIG, OspfMaxMetricConfigReader(underlayAccess))
        rRegistry.add(IIDs.NE_NE_PR_PR_OS_AR_AREA, OspfAreaReader(underlayAccess))
        rRegistry.add(IIDs.NE_NE_PR_PR_OS_AR_AR_CONFIG, OspfAreaConfigReader())
        rRegistry.add(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_INTERFACE, OspfAreaInterfaceReader(underlayAccess))
        rRegistry.add(IIDs.NE_NE_PR_PR_OS_AR_AR_IN_IN_CONFIG, OspfAreaInterfaceConfigReader(underlayAccess))
    }

    override fun toString(): String = "Junos 17.3 ospf translate unit"
}