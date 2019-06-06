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

package io.frinx.unitopo.unit.xr66.interfaces

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import io.frinx.unitopo.unit.xr66.init.Unit
import io.frinx.unitopo.unit.xr66.interfaces.handler.InterfaceConfigReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.InterfaceConfigWriter
import io.frinx.unitopo.unit.xr66.interfaces.handler.InterfaceDampingConfigReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.InterfaceDampingConfigWriter
import io.frinx.unitopo.unit.xr66.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.InterfaceStatisticsConfigReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.InterfaceStatisticsConfigWriter
import io.frinx.unitopo.unit.xr66.interfaces.handler.aggregate.AggregateConfigReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.aggregate.AggregateConfigWriter
import io.frinx.unitopo.unit.xr66.interfaces.handler.ethernet.EthernetConfigReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.ethernet.EthernetConfigWriter
import io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.Ipv4AddressReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.Ipv4ConfigReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.Ipv4ConfigWriter
import io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.SubinterfaceConfigReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.SubinterfaceConfigWriter
import io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.SubinterfaceReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.SubinterfaceStatisticsConfigReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.SubinterfaceStatisticsConfigWriter
import io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.holdtime.HoldTimeConfigReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.holdtime.HoldTimeConfigWriter
import io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.ipv4.Ipv4MtuConfigReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.ipv4.Ipv4MtuConfigWriter
import io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.vlan.SubinterfaceVlanConfigReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.vlan.SubinterfaceVlanConfigWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.IfDampAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.DampingBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.AggregationBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.IfCiscoStatsAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.IfSubifCiscoHoldTimeAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.StatisticsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.Interface1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.EthernetBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.Ipv4Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.AddressesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.HoldTimeBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.VlanBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import io.frinx.openconfig.openconfig._if.ip.IIDs as IfIpIIDs
import io.frinx.openconfig.openconfig.network.instance.IIDs as NetworkInstanceIIDs
import io.frinx.openconfig.openconfig.vlan.IIDs as VlanIIDs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501.`$YangModuleInfoImpl` as UnderlayBundleMgrYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.statsd.cfg.rev170501.`$YangModuleInfoImpl` as UnderlayInfraStatsdInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev180111.`$YangModuleInfoImpl` as UnderlayIpv4YangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev180615.`$YangModuleInfoImpl` as UnderlayInfraYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.`$YangModuleInfoImpl` as UnderlayDatatypesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.mdrv.lib.cfg.rev151109.`$YangModuleInfoImpl` as UnderlayMdrvLibYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.`$YangModuleInfoImpl` as UnderlayTypesYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.`$YangModuleInfoImpl` as DampingYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.ext.rev180926.`$YangModuleInfoImpl` as AggregateExtYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Interface1Builder as AggregateInterface1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.`$YangModuleInfoImpl` as AggregateYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.IfSubifCiscoStatsAugBuilder as SubInt1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.`$YangModuleInfoImpl` as StatisticsYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.`$YangModuleInfoImpl` as EthernetYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.`$YangModuleInfoImpl` as IpYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.`$YangModuleInfoImpl` as InterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.lag.member.rev171109.`$YangModuleInfoImpl` as LacpLagMemberYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.`$YangModuleInfoImpl` as LacpYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.Subinterface1Builder as VlanAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.`$YangModuleInfoImpl` as VlanYangInfo
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
        InterfaceTypesYangInfo.getInstance(),
        StatisticsYangInfo.getInstance(),
        IpYangInfo.getInstance(),
        VlanYangInfo.getInstance(),
        LacpYangInfo.getInstance(),
        LacpLagMemberYangInfo.getInstance(),
        EthernetYangInfo.getInstance(),
        AggregateYangInfo.getInstance(),
        DampingYangInfo.getInstance(),
        AggregateExtYangInfo.getInstance()
    )

    override fun getUnderlayYangSchemas() = UNDERLAY_SCHEMAS

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
        wRegistry.add(GenericListWriter(IIDs.IN_INTERFACE, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_CONFIG, InterfaceConfigWriter(underlayAccess)))

        wRegistry.add(GenericListWriter(IIDs.IN_IN_SU_SUBINTERFACE, NoopListWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_SU_SU_CONFIG, SubinterfaceConfigWriter(underlayAccess)),
            IIDs.IN_IN_CONFIG)
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_AUG_IFCISCOSTATSAUG_ST_CONFIG,
            InterfaceStatisticsConfigWriter(underlayAccess)),
            IIDs.IN_IN_CONFIG)
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_AUG_IFDAMPAUG_DA_CONFIG,
            InterfaceDampingConfigWriter(underlayAccess)),
            IIDs.IN_IN_CONFIG)
        wRegistry.addAfter(GenericWriter(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VL_CONFIG,
            SubinterfaceVlanConfigWriter(underlayAccess)), IIDs.IN_IN_SU_SU_CONFIG)

        wRegistry.add(GenericWriter(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOSTATSAUG, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOSTATSAUG_STATISTICS, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOSTATSAUG_ST_CONFIG,
            SubinterfaceStatisticsConfigWriter(underlayAccess)),
            IIDs.IN_IN_SU_SU_CONFIG)

        wRegistry.addAfter(GenericWriter(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_CONFIG,
                Ipv4MtuConfigWriter(underlayAccess)),
                setOf(IIDs.IN_IN_SU_SU_CONFIG, IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IPV4))
        wRegistry.add(GenericWriter(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_ADDRESS, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_AD_CONFIG,
            Ipv4ConfigWriter(underlayAccess)), NetworkInstanceIIDs.NE_NE_IN_IN_CONFIG)

        // if-ethernet
        wRegistry.add(GenericWriter(IIDs.IN_IN_AUG_INTERFACE1_ETHERNET, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_ET_CO_AUG_CONFIG1, NoopWriter()))
        wRegistry.subtreeAddAfter(setOf(
            RWUtils.cutIdFromStart(IIDs.INT_INT_ETH_CON_AUG_CONFIG1, IIDs.IN_IN_AUG_INTERFACE1_ET_CONFIG),
            RWUtils.cutIdFromStart(io.frinx.openconfig.openconfig.lacp.IIDs.IN_IN_ET_CO_AUG_LACPETHCONFIGAUG,
                IIDs.IN_IN_AUG_INTERFACE1_ET_CONFIG)),
            GenericWriter(IIDs.IN_IN_AUG_INTERFACE1_ET_CONFIG, EthernetConfigWriter(underlayAccess)),
            IIDs.IN_IN_CONFIG)

        // if-aggregation
        wRegistry.add(GenericWriter(IIDs.INT_INT_AUG_INTERFACE1, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_AUG_INTERFACE1_AGGREGATION, NoopWriter()))
        wRegistry.subtreeAddAfter(setOf(
                RWUtils.cutIdFromStart(IIDs.IN_IN_AG_CO_AUG_IFLAGAUG, InstanceIdentifier.create(Config::class.java))),
                GenericWriter(IIDs.IN_IN_AUG_INTERFACE1_AG_CONFIG, AggregateConfigWriter(underlayAccess)),
                IIDs.IN_IN_CONFIG)

        // hold-time(sub-interface)
        wRegistry.add(GenericWriter(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOHOLDTIMEAUG, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOHOLDTIMEAUG_HOLDTIME, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOHOLDTIMEAUG_HO_CONFIG,
            HoldTimeConfigWriter(underlayAccess)),
            IIDs.IN_IN_SU_SU_CONFIG)
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess) {
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
        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_IFDAMPAUG, IfDampAugBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_IFDAMPAUG_DAMPING, DampingBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_AUG_IFDAMPAUG_DA_CONFIG,
            InterfaceDampingConfigReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOSTATSAUG, SubInt1Builder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOSTATSAUG_STATISTICS,
            StatisticsBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOSTATSAUG_ST_CONFIG,
            SubinterfaceStatisticsConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1, VlanAugBuilder::class.java)
        rRegistry.addStructuralReader(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VLAN, VlanBuilder::class.java)
        rRegistry.add(GenericConfigReader(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VL_CONFIG,
            SubinterfaceVlanConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1, Subinterface1Builder::class.java)
        rRegistry.addStructuralReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IPV4, Ipv4Builder::class.java)
        rRegistry.addStructuralReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_ADDRESSES, AddressesBuilder::class.java)
        rRegistry.add(GenericConfigReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_CONFIG,
            Ipv4MtuConfigReader(underlayAccess)))
        rRegistry.add(GenericConfigListReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_ADDRESS,
            Ipv4AddressReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_AD_CONFIG,
            Ipv4ConfigReader(underlayAccess)))

        // if-ethernet
        rRegistry.addStructuralReader(IIDs.INTER_INTER_AUG_INTERFACE1, Interface1Builder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_INTERFACE1_ETHERNET, EthernetBuilder::class.java)
        rRegistry.subtreeAdd(setOf(
            RWUtils.cutIdFromStart(IIDs.INT_INT_ETH_CON_AUG_CONFIG1, IIDs.IN_IN_AUG_INTERFACE1_ET_CONFIG),
            RWUtils.cutIdFromStart(io.frinx.openconfig.openconfig.lacp.IIDs.IN_IN_ET_CO_AUG_CONFIG1,
                IIDs.IN_IN_AUG_INTERFACE1_ET_CONFIG)),
            GenericConfigReader(IIDs.IN_IN_AUG_INTERFACE1_ET_CONFIG, EthernetConfigReader(underlayAccess)))

        // if-aggregation
        rRegistry.addStructuralReader(IIDs.INT_INT_AUG_INTERFACE1, AggregateInterface1Builder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_INTERFACE1_AGGREGATION, AggregationBuilder::class.java)
        rRegistry.subtreeAdd(setOf(
                RWUtils.cutIdFromStart(IIDs.IN_IN_AG_CO_AUG_IFLAGAUG, InstanceIdentifier.create(Config::class.java))),
                GenericConfigReader(IIDs.IN_IN_AUG_INTERFACE1_AG_CONFIG, AggregateConfigReader(underlayAccess)))

        // hold-time(sub-interface)
        rRegistry.addStructuralReader(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOHOLDTIMEAUG,
            IfSubifCiscoHoldTimeAugBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOHOLDTIMEAUG_HOLDTIME,
            HoldTimeBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOHOLDTIMEAUG_HO_CONFIG,
            HoldTimeConfigReader(underlayAccess)))
    }

    override fun toString(): String = "Cisco-IOS-XR-ifmgr-cfg@2017-09-07 interface translate unit"

    companion object {
        val UNDERLAY_SCHEMAS = setOf(
            UnderlayInterfacesYangInfo.getInstance(),
            UnderlayDatatypesYangInfo.getInstance(),
            UnderlayInfraYangInfo.getInstance(),
            UnderlayInfraStatsdInfo.getInstance(),
            UnderlayIpv4YangInfo.getInstance(),
            UnderlayTypesYangInfo.getInstance(),
            UnderlayBundleMgrYangInfo.getInstance(),
            UnderlayMdrvLibYangInfo.getInstance()
        )
    }
}