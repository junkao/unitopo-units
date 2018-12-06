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
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.openconfig.openconfig.lacp.IIDs as LacpIIDs
import io.frinx.openconfig.openconfig._if.ip.IIDs as IfIpIIDs
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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.IfDampAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.DampingBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.AggregationBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.IfLagBfdAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.bfd.top.BfdBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.Interface1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.EthernetBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.Ipv4Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.AddressesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.HoldTimeBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.`$YangModuleInfoImpl` as DampingYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Interface1Builder as AggregateInterface1AugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.`$YangModuleInfoImpl` as AggregateYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.`$YangModuleInfoImpl` as LagBfdYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.`$YangModuleInfoImpl` as OpenConfEthCfgYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.ethernet.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.`$YangModuleInfoImpl` as IpYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.rev171024.`$YangModuleInfoImpl` as IfLagJuniperAugYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.`$YangModuleInfoImpl` as InterfacesYangInfo
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
        wRegistry.add(GenericWriter(IIDs.IN_IN_AUG_IFDAMPAUG, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_AUG_IFDAMPAUG_DAMPING, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_AUG_IFDAMPAUG_DA_CONFIG,
            InterfaceDampingConfigWriter(underlayAccess)), IIDs.IN_IN_CONFIG)

        wRegistry.add(GenericWriter(IIDs.INTE_INTE_AUG_INTERFACE1, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_AUG_INTERFACE1_ETHERNET, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_AUG_INTERFACE1_ET_CONFIG, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_ET_CO_AUG_CONFIG1,
            InterfaceIfAggregateConfigWriter(underlayAccess)), IIDs.IN_IN_CONFIG)
        wRegistry.addAfter(GenericWriter(io.frinx.openconfig.openconfig.lacp.IIDs.IN_IN_ET_CO_AUG_LACPETHCONFIGAUG,
            InterfaceLacpConfigWriter(underlayAccess)), IIDs.IN_IN_CONFIG)

        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_SUBINTERFACES, NoopWriter()), IIDs.IN_IN_CONFIG)
        wRegistry.addAfter(GenericListWriter(IIDs.IN_IN_SU_SUBINTERFACE, NoopListWriter()), IIDs.IN_IN_SUBINTERFACES)
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_SU_SU_CONFIG, SubinterfaceConfigWriter(underlayAccess)),
            IIDs.IN_IN_SU_SUBINTERFACE)
        wRegistry.addAfter(GenericWriter(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1, NoopWriter()), IIDs.IN_IN_SU_SU_CONFIG)
        wRegistry.addAfter(GenericWriter(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IPV4, NoopWriter()),
            IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1)
        wRegistry.addAfter(GenericWriter(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_ADDRESSES, NoopWriter()),
            IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IPV4)
        wRegistry.addAfter(GenericListWriter(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_ADDRESS, NoopListWriter()),
            IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_ADDRESSES)
        wRegistry.addAfter(GenericWriter(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_AD_CONFIG,
            SubinterfaceAddressConfigWriter(underlayAccess)), IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_ADDRESS)

        wRegistry.addAfter(GenericWriter(IIDs.INTER_INTER_AUG_INTERFACE1, NoopWriter()), IIDs.IN_IN_CONFIG)
        wRegistry.add(GenericWriter(IIDs.IN_IN_AUG_INTERFACE1_AGGREGATION, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_AG_AUG_IFLAGBFDAUG, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_AG_AUG_IFLAGBFDAUG_BFD, NoopWriter()))
        wRegistry.subtreeAdd(setOf(RWUtils.cutIdFromStart(IIDs.IN_IN_AG_CO_AUG_IFLAGJUNIPERAUG, IFC_AGGREGATE_IID)),
            GenericWriter(IIDs.IN_IN_AUG_INTERFACE1_AG_CONFIG, InterfaceAggregationConfigWriter(underlayAccess)))
        wRegistry.add(GenericWriter(IIDs.IN_IN_AG_AUG_IFLAGBFDAUG_BF_CONFIG,
            InterfaceAggregationBfdConfigWriter(underlayAccess)))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.IN_INTERFACE, InterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_CONFIG, InterfaceConfigReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.IN_IN_HOLDTIME, HoldTimeBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_HO_CONFIG, InterfaceHoldTimeConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_IFDAMPAUG, IfDampAugBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_IFDAMPAUG_DAMPING, DampingBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_AUG_IFDAMPAUG_DA_CONFIG,
            InterfaceDampingConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IIDs.INTER_INTER_AUG_INTERFACE1, Interface1Builder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_INTERFACE1_ETHERNET, EthernetBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_INTERFACE1_ET_CONFIG, ConfigBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_ET_CO_AUG_CONFIG1,
            InterfaceIfAggregateConfigReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(LacpIIDs.IN_IN_ET_CO_AUG_CONFIG1, InterfaceLacpConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IIDs.IN_IN_SUBINTERFACES, SubinterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.IN_IN_SU_SUBINTERFACE, SubinterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_SU_SU_CONFIG, SubinterfaceConfigReader(underlayAccess)))
        rRegistry.addStructuralReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1, Subinterface1Builder::class.java)
        rRegistry.addStructuralReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IPV4, Ipv4Builder::class.java)
        rRegistry.addStructuralReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_ADDRESSES, AddressesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_ADDRESS,
            SubinterfaceAddressReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_AD_CONFIG,
            SubinterfaceAddressConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IIDs.INT_INT_AUG_INTERFACE1, AggregateInterface1AugBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_INTERFACE1_AGGREGATION, AggregationBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_AG_AUG_IFLAGBFDAUG, IfLagBfdAugBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_AG_AUG_IFLAGBFDAUG_BFD, BfdBuilder::class.java)
        rRegistry.subtreeAdd(setOf(
            RWUtils.cutIdFromStart(IIDs.IN_IN_AG_CO_AUG_IFLAGJUNIPERAUG, IFC_AGGREGATE_IID)),
            GenericConfigReader(IIDs.IN_IN_AUG_INTERFACE1_AG_CONFIG, InterfaceAggregationConfigReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_AG_AUG_IFLAGBFDAUG_BF_CONFIG,
            InterfaceAggregationBfdConfigReader(underlayAccess)))
    }

    override fun toString(): String = "Junos 17.3 interface translate unit"

    companion object {
        private val IFC_AGGREGATE_IID = InstanceIdentifier.create(Config::class.java)
    }
}