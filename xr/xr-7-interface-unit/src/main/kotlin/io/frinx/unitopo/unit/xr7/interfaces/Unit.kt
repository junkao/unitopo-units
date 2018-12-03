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

package io.frinx.unitopo.unit.xr7.interfaces

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.xr7.init.Unit
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceConfigReader
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceConfigWriter
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceStatisticsConfigReader
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceStatisticsConfigWriter
import io.frinx.unitopo.unit.xr7.interfaces.handler.subifc.SubinterfaceConfigReader
import io.frinx.unitopo.unit.xr7.interfaces.handler.subifc.SubinterfaceConfigWriter
import io.frinx.unitopo.unit.xr7.interfaces.handler.subifc.SubinterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.IfCiscoStatsAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.StatisticsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.statsd.cfg.rev170501.`$YangModuleInfoImpl` as UnderlayInfraStatsdInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev180615.`$YangModuleInfoImpl` as UnderlayInfraYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.`$YangModuleInfoImpl` as UnderlayDatatypesYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.`$YangModuleInfoImpl` as StatisticsYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.`$YangModuleInfoImpl` as InterfacesYangInfo
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.`$YangModuleInfoImpl` as InterfaceTypesYangInfo

class Unit(private val registry: TranslationUnitCollector) : Unit() {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas() = setOf(
        InterfacesYangInfo.getInstance(),
        UnderlayInterfacesYangInfo.getInstance(),
        InterfaceTypesYangInfo.getInstance(),
        StatisticsYangInfo.getInstance()
    )

    override fun getUnderlayYangSchemas() = UNDERLAY_SCHEMAS

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
        wRegistry.add(GenericListWriter(IIDs.IN_INTERFACE, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_CONFIG, InterfaceConfigWriter(underlayAccess)))

        wRegistry.add(GenericListWriter(IIDs.IN_IN_SU_SUBINTERFACE, NoopListWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_SU_SU_CONFIG, SubinterfaceConfigWriter(underlayAccess)),
            IIDs.IN_IN_CONFIG)
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_AUG_IFCISCOSTATSAUG_ST_CONFIG,
            InterfaceStatisticsConfigWriter(underlayAccess)),
            IIDs.IN_IN_CONFIG)
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.IN_INTERFACE, InterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_CONFIG, InterfaceConfigReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_IFCISCOSTATSAUG, IfCiscoStatsAugBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_IFCISCOSTATSAUG_STATISTICS, StatisticsBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_AUG_IFCISCOSTATSAUG_ST_CONFIG,
            InterfaceStatisticsConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IIDs.IN_IN_SUBINTERFACES, SubinterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.IN_IN_SU_SUBINTERFACE, SubinterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_SU_SU_CONFIG, SubinterfaceConfigReader(underlayAccess)))
    }

    override fun toString(): String = "XR 7 (2018-08-13) interface translate unit"

    companion object {
        val UNDERLAY_SCHEMAS = setOf(
            UnderlayInterfacesYangInfo.getInstance(),
            UnderlayDatatypesYangInfo.getInstance(),
            UnderlayInfraYangInfo.getInstance(),
            UnderlayInfraStatsdInfo.getInstance()
        )
    }
}