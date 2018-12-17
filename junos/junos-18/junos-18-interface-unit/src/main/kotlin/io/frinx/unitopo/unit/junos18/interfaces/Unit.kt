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

package io.frinx.unitopo.unit.junos18.interfaces

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.openconfig.openconfig.vlan.IIDs as VlanIIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceConfigReader
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceConfigWriter
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceConfigReader
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceConfigWriter
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceReader
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceVlanConfigReader
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceVlanConfigWriter
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.Subinterface1Builder as SubinterfaceVlanAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.VlanBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.`$YangModuleInfoImpl` as InterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.`$YangModuleInfoImpl` as VlanYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.root.rev180101.`$YangModuleInfoImpl` as UnderlayConfRootYangModuleInfo
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.`$YangModuleInfoImpl` as IanaIfTypeYangInfo

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas() = setOf(
            InterfacesYangInfo.getInstance(),
            VlanYangInfo.getInstance(),
            IanaIfTypeYangInfo.getInstance()
    )
    override fun getUnderlayYangSchemas() = setOf(
            UnderlayConfRootYangModuleInfo.getInstance(),
            UnderlayInterfacesYangInfo.getInstance()
    )

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

        wRegistry.add(GenericWriter(IIDs.IN_IN_SUBINTERFACES, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.IN_IN_SU_SUBINTERFACE, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_SU_SU_CONFIG, SubinterfaceConfigWriter(underlayAccess)))

        wRegistry.add(GenericWriter(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1, NoopWriter()))
        wRegistry.add(GenericWriter(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VLAN, NoopWriter()))
        wRegistry.addAfter(GenericWriter(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VL_CONFIG,
                SubinterfaceVlanConfigWriter(underlayAccess)), IIDs.IN_IN_SU_SU_CONFIG)
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.IN_INTERFACE, InterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_CONFIG, InterfaceConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IIDs.IN_IN_SUBINTERFACES, SubinterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.IN_IN_SU_SUBINTERFACE, SubinterfaceReader(underlayAccess)))
        rRegistry.add(
            GenericConfigReader(IIDs.IN_IN_SU_SU_CONFIG, SubinterfaceConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1, SubinterfaceVlanAugBuilder::class.java)
        rRegistry.addStructuralReader(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VLAN, VlanBuilder::class.java)
        rRegistry.add(GenericConfigReader(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VL_CONFIG,
                SubinterfaceVlanConfigReader(underlayAccess)))
    }

    override fun toString(): String = "Junos 18.2 interface translate unit"
}