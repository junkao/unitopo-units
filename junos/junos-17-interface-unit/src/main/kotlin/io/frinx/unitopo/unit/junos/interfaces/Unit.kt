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
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceHoldTimeConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceHoldTimeConfigWriter
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceIfAggregateConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceIfAggregateConfigWriter
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceLacpConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceLacpConfigWriter
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceWriter
import io.frinx.unitopo.unit.junos.interfaces.handler.lag.aggregate.InterfaceAggregationBfdConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.lag.aggregate.InterfaceAggregationBfdConfigWriter
import io.frinx.unitopo.unit.junos.interfaces.handler.lag.aggregate.InterfaceAggregationConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.lag.aggregate.InterfaceAggregationConfigWriter
import io.frinx.unitopo.unit.junos.interfaces.handler.subinterfaces.SubinterfaceAddressConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.subinterfaces.SubinterfaceAddressConfigWriter
import io.frinx.unitopo.unit.junos.interfaces.handler.subinterfaces.SubinterfaceAddressReader
import io.frinx.unitopo.unit.junos.interfaces.handler.subinterfaces.SubinterfaceConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.subinterfaces.SubinterfaceConfigWriter
import io.frinx.unitopo.unit.junos.interfaces.handler.subinterfaces.SubinterfaceReader
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.IfDampAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.IfDampAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.Damping
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.DampingBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.damping.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.Aggregation
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.AggregationBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.IfLagBfdAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.IfLagBfdAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.bfd.top.Bfd
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.bfd.top.BfdBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.Interface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.Interface1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.Ethernet
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.EthernetBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.Ipv4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.Ipv4Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.Addresses
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.AddressesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.rev171024.IfLagJuniperAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.HoldTimeBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.lag.member.rev171109.LacpEthConfigAug
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.`$YangModuleInfoImpl` as DampingYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Config1 as AgIdConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Interface1 as AggregateInterface1Aug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Interface1Builder as AggregateInterface1AugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.`$YangModuleInfoImpl` as AggregateYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.Config as AggregationConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.`$YangModuleInfoImpl` as LagBfdYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.bfd.top.bfd.Config as BfdConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.`$YangModuleInfoImpl` as OpenConfEthCfgYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.ethernet.Config as EthernetConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.ethernet.ConfigBuilder as EthernetConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.`$YangModuleInfoImpl` as IpYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.address.Config as AddressConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.rev171024.`$YangModuleInfoImpl` as IfLagJuniperAugYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.`$YangModuleInfoImpl` as InterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.lag.member.rev171109.Config1 as LacpConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.lag.member.rev171109.`$YangModuleInfoImpl` as LacpAugYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.`$YangModuleInfoImpl` as LacpYangInfo
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
            OpenConfEthCfgYangInfo.getInstance(),
            AggregateYangInfo.getInstance(),
            LagBfdYangInfo.getInstance(),
            IfLagJuniperAugYangInfo.getInstance(),
            LacpYangInfo.getInstance(),
            LacpAugYangInfo.getInstance())

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
        wRegistry.add(GenericListWriter(IIDs.IN_INTERFACE, InterfaceWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_CONFIG, InterfaceConfigWriter(underlayAccess)))
        wRegistry.add(GenericWriter(IIDs.IN_IN_HOLDTIME, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_HO_CONFIG, InterfaceHoldTimeConfigWriter(underlayAccess)),
            IIDs.IN_IN_CONFIG)
        wRegistry.add(GenericWriter(IFC_Damping_AUG_ID, NoopWriter()))
        wRegistry.add(GenericWriter(IFC_Damping_ID, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IFC_Damping_CFG_ID, InterfaceDampingConfigWriter(underlayAccess)),
            IIDs.IN_IN_CONFIG)

        wRegistry.add(GenericWriter(IFC_ETHERNET_AUG_ID, NoopWriter()))
        wRegistry.add(GenericWriter(IFC_ETHERNET_ID, NoopWriter()))
        wRegistry.add(GenericWriter(IFC_ETHERNET_CFG_ID, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IFC_ETHERNET_CFG_AUG_AG_ID, InterfaceIfAggregateConfigWriter(underlayAccess)),
            IIDs.IN_IN_CONFIG)
        wRegistry.addAfter(GenericWriter(IFC_ETHERNET_CFG_ID.augmentation(LacpEthConfigAug::class.java),
            InterfaceLacpConfigWriter(underlayAccess)), IIDs.IN_IN_CONFIG)

        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_SUBINTERFACES, NoopWriter()), IIDs.IN_IN_CONFIG)
        wRegistry.addAfter(GenericListWriter(IIDs.IN_IN_SU_SUBINTERFACE, NoopListWriter()), IIDs.IN_IN_SUBINTERFACES)
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_SU_SU_CONFIG, SubinterfaceConfigWriter(underlayAccess)),
            IIDs.IN_IN_SU_SUBINTERFACE)
        wRegistry.addAfter(GenericWriter(IFC_SUBIFC_IPV4_AUG, NoopWriter()), IIDs.IN_IN_SU_SU_CONFIG)
        wRegistry.addAfter(GenericWriter(IFC_SUBIFC_IPV4, NoopWriter()), IFC_SUBIFC_IPV4_AUG)
        wRegistry.addAfter(GenericWriter(IFC_SUBIFC_IPV4_ADDRESSES, NoopWriter()), IFC_SUBIFC_IPV4)
        wRegistry.addAfter(GenericListWriter(IFC_SUBIFC_IPV4_ADDRESSES_ADDR, NoopListWriter()),
            IFC_SUBIFC_IPV4_ADDRESSES)
        wRegistry.addAfter(GenericWriter(IFC_SUBIFC_IPV4_ADDRESSES_ADDR_CFG,
            SubinterfaceAddressConfigWriter(underlayAccess)), IFC_SUBIFC_IPV4_ADDRESSES_ADDR)

        wRegistry.addAfter(GenericWriter(IFC_AGGREGATE_AUG, NoopWriter()), IIDs.IN_IN_CONFIG)
        wRegistry.add(GenericWriter(IFC_AGGREGATE_ID, NoopWriter()))
        wRegistry.add(GenericWriter(IFC_AGGREGATE_BFD_AUG, NoopWriter()))
        wRegistry.add(GenericWriter(IFC_AGGREGATE_BFD_ID, NoopWriter()))
        wRegistry.subtreeAdd(setOf(IFC_AGGREGATE_CFG_AUG), GenericWriter(IFC_AGGREGATE_CFG,
            InterfaceAggregationConfigWriter(underlayAccess)))
        wRegistry.add(GenericWriter(IFC_AGGREGATE_BFD_CFG, InterfaceAggregationBfdConfigWriter(underlayAccess)))
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
        rRegistry.add(GenericConfigReader(IFC_ETHERNET_CFG_AUG_AG_ID,
            InterfaceIfAggregateConfigReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IFC_ETHERNET_CFG_AUG_LACP_ID, InterfaceLacpConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IIDs.IN_IN_SUBINTERFACES, SubinterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.IN_IN_SU_SUBINTERFACE, SubinterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_SU_SU_CONFIG, SubinterfaceConfigReader(underlayAccess)))
        rRegistry.addStructuralReader(IFC_SUBIFC_IPV4_AUG, Subinterface1Builder::class.java)
        rRegistry.addStructuralReader(IFC_SUBIFC_IPV4, Ipv4Builder::class.java)
        rRegistry.addStructuralReader(IFC_SUBIFC_IPV4_ADDRESSES, AddressesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IFC_SUBIFC_IPV4_ADDRESSES_ADDR,
            SubinterfaceAddressReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IFC_SUBIFC_IPV4_ADDRESSES_ADDR_CFG,
            SubinterfaceAddressConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IFC_AGGREGATE_AUG, AggregateInterface1AugBuilder::class.java)
        rRegistry.addStructuralReader(IFC_AGGREGATE_ID, AggregationBuilder::class.java)
        rRegistry.addStructuralReader(IFC_AGGREGATE_BFD_AUG, IfLagBfdAugBuilder::class.java)
        rRegistry.addStructuralReader(IFC_AGGREGATE_BFD_ID, BfdBuilder::class.java)
        rRegistry.subtreeAdd(setOf(IFC_AGGREGATE_CFG_AUG), GenericConfigReader(IFC_AGGREGATE_CFG,
            InterfaceAggregationConfigReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IFC_AGGREGATE_BFD_CFG, InterfaceAggregationBfdConfigReader(underlayAccess)))
    }

    override fun toString(): String = "Junos 17.3 interface translate unit"

    companion object {
        private val IFC_Damping_AUG_ID = IIDs.IN_INTERFACE.augmentation(IfDampAug::class.java)
        private val IFC_Damping_ID = IFC_Damping_AUG_ID.child(Damping::class.java)
        private val IFC_Damping_CFG_ID = IFC_Damping_ID.child(Config::class.java)

        private val IFC_ETHERNET_AUG_ID = IIDs.IN_INTERFACE.augmentation(Interface1::class.java)
        private val IFC_ETHERNET_ID = IFC_ETHERNET_AUG_ID.child(Ethernet::class.java)
        private val IFC_ETHERNET_CFG_ID = IFC_ETHERNET_ID.child(EthernetConfig::class.java)
        private val IFC_ETHERNET_CFG_AUG_AG_ID = IFC_ETHERNET_CFG_ID.augmentation(AgIdConfig::class.java)
        private val IFC_ETHERNET_CFG_AUG_LACP_ID = IFC_ETHERNET_CFG_ID.augmentation(LacpConfig::class.java)

        private val IFC_SUBIFC_IPV4_AUG = IIDs.IN_IN_SU_SUBINTERFACE.augmentation(Subinterface1::class.java)
        private val IFC_SUBIFC_IPV4 = IFC_SUBIFC_IPV4_AUG.child(Ipv4::class.java)
        private val IFC_SUBIFC_IPV4_ADDRESSES = IFC_SUBIFC_IPV4.child(Addresses::class.java)
        private val IFC_SUBIFC_IPV4_ADDRESSES_ADDR = IFC_SUBIFC_IPV4_ADDRESSES.child(Address::class.java)
        private val IFC_SUBIFC_IPV4_ADDRESSES_ADDR_CFG = IFC_SUBIFC_IPV4_ADDRESSES_ADDR.child(AddressConfig::class.java)

        private val IFC_AGGREGATE_AUG = IIDs.IN_INTERFACE.augmentation(AggregateInterface1Aug::class.java)
        private val IFC_AGGREGATE_ID = IFC_AGGREGATE_AUG.child(Aggregation::class.java)
        private val IFC_AGGREGATE_CFG = IFC_AGGREGATE_ID.child(AggregationConfig::class.java)
        private val IFC_AGGREGATE_CFG_AUG = InstanceIdentifier.create(AggregationConfig::class.java)
            .augmentation(IfLagJuniperAug::class.java)
        private val IFC_AGGREGATE_BFD_AUG = IFC_AGGREGATE_ID.augmentation(IfLagBfdAug::class.java)
        private val IFC_AGGREGATE_BFD_ID = IFC_AGGREGATE_BFD_AUG.child(Bfd::class.java)
        private val IFC_AGGREGATE_BFD_CFG = IFC_AGGREGATE_BFD_ID.child(BfdConfig::class.java)
    }
}