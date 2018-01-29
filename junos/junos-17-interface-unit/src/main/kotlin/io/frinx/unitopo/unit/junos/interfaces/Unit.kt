/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.interfaces

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceConfigWriter
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceDampingConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceDampingConfigWriter
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceEthernetConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceEthernetConfigWriter
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceHoldTimeConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceHoldTimeConfigWriter
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.IfDampAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.IfDampAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.Damping
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.DampingBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.damping.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Config1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.Interface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.Interface1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.Ethernet
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.EthernetBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.HoldTimeBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.`$YangModuleInfoImpl` as DampingYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.`$YangModuleInfoImpl` as OpenConfEthCfgYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.ethernet.Config as EthernetConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.ethernet.ConfigBuilder as EthernetConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.`$YangModuleInfoImpl` as IpYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.`$YangModuleInfoImpl` as InterfacesYangInfo
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

            InterfacesYangInfo.getInstance(),
            IpYangInfo.getInstance(),
            DampingYangInfo.getInstance(),
            OpenConfEthCfgYangInfo.getInstance())

    override fun getUnderlayYangSchemas() = setOf(
            UnderlayInterfacesYangInfo.getInstance())

    override fun getRpcs(underlayAccess: UnderlayAccess) = emptySet<RpcService<*, *>>()

    override fun provideHandlers(rRegistry: ModifiableReaderRegistryBuilder,
                                 wRegistry: ModifiableWriterRegistryBuilder,
                                 underlayAccess: UnderlayAccess) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.add(GenericListWriter(IIDs.IN_INTERFACE, InterfaceWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_CONFIG, InterfaceConfigWriter(underlayAccess)))
        wRegistry.add(GenericWriter(IIDs.IN_IN_HOLDTIME, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_HO_CONFIG, InterfaceHoldTimeConfigWriter(underlayAccess)))
        wRegistry.add(GenericWriter(IFC_Damping_AUG_ID, NoopWriter()))
        wRegistry.add(GenericWriter(IFC_Damping_ID, NoopWriter()))
        wRegistry.add(GenericWriter(IFC_Damping_CFG_ID, InterfaceDampingConfigWriter(underlayAccess)))

        wRegistry.add(GenericWriter(IFC_ETHERNET_AUG_ID, NoopWriter()))
        wRegistry.add(GenericWriter(IFC_ETHERNET_ID, NoopWriter()))
        wRegistry.add(GenericWriter(IFC_ETHERNET_CFG_ID, NoopWriter()))
        wRegistry.add(GenericWriter(IFC_ETHERNET_CFG_AUG_ID, InterfaceEthernetConfigWriter(underlayAccess)))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.IN_INTERFACE, InterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_CONFIG, InterfaceConfigReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.IN_IN_HOLDTIME, HoldTimeBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_HO_CONFIG, InterfaceHoldTimeConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IFC_Damping_AUG_ID, IfDampAugBuilder::class.java)
        rRegistry.addStructuralReader(IFC_Damping_ID, DampingBuilder::class.java)
        rRegistry.add(GenericConfigReader(IFC_Damping_CFG_ID, InterfaceDampingConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IFC_ETHERNET_AUG_ID, Interface1Builder::class.java)
        rRegistry.addStructuralReader(IFC_ETHERNET_ID, EthernetBuilder::class.java)
        rRegistry.addStructuralReader(IFC_ETHERNET_CFG_ID, EthernetConfigBuilder::class.java)
        rRegistry.add(GenericConfigReader(IFC_ETHERNET_CFG_AUG_ID, InterfaceEthernetConfigReader(underlayAccess)))
    }

    override fun toString(): String = "Junos 17.3 interface translate unit"

    companion object {
        private val IFC_Damping_AUG_ID = IIDs.IN_INTERFACE.augmentation(IfDampAug::class.java)
        private val IFC_Damping_ID = IFC_Damping_AUG_ID.child(Damping::class.java)
        private val IFC_Damping_CFG_ID = IFC_Damping_ID.child(Config::class.java)

        private val IFC_ETHERNET_AUG_ID = IIDs.IN_INTERFACE.augmentation(Interface1::class.java)
        private val IFC_ETHERNET_ID = IFC_ETHERNET_AUG_ID.child(Ethernet::class.java)
        private val IFC_ETHERNET_CFG_ID = IFC_ETHERNET_ID.child(EthernetConfig::class.java)
        private val IFC_ETHERNET_CFG_AUG_ID = IFC_ETHERNET_CFG_ID.augmentation(Config1::class.java)
    }
}
