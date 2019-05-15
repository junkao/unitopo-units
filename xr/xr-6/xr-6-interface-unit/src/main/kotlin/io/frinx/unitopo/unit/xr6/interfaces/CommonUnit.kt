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

package io.frinx.unitopo.unit.xr6.interfaces

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.read.GenericOperReader
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.init.Unit
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceConfigReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceConfigWriter
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceDampeningConfigReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceDampeningConfigWriter
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceStateReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceStatisticsConfigReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceStatisticsConfigWriter
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.Ipv4AddressConfigReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.Ipv4AddressConfigWriter
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.Ipv4AddressReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.Ipv4AddressWriter
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.SubinterfaceConfigReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.SubinterfaceConfigWriter
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.SubinterfaceReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.SubinterfaceStateReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.SubinterfaceStatisticsConfigReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.SubinterfaceStatisticsConfigWriter
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ipv4.Ipv4MtuConfigReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ipv4.Ipv4MtuConfigWriter
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.vlan.SubinterfaceVlanConfigReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.vlan.SubinterfaceVlanConfigWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.IfDampAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.DampingBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.IfCiscoStatsAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.IfSubifCiscoStatsAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.StatisticsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.Ipv4Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.AddressesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.VlanBuilder
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import io.frinx.openconfig.openconfig._if.ip.IIDs as IfIpIIDs
import io.frinx.openconfig.openconfig.network.instance.IIDs as NetworkInstanceIIDs
import io.frinx.openconfig.openconfig.vlan.IIDs as VlanIIDs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730.`$YangModuleInfoImpl` as UnderlayInterfacesOperYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.statsd.cfg.rev151109.`$YangModuleInfoImpl` as UnderlayInfraStatsdInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayIpv4YangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.`$YangModuleInfoImpl` as UnderlayTypesYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.`$YangModuleInfoImpl` as DampingYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.`$YangModuleInfoImpl` as StatisticsYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.`$YangModuleInfoImpl` as IpYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.`$YangModuleInfoImpl` as InterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.Subinterface1Builder as VlanAugBuilder

abstract class CommonUnit(private val registry: TranslationUnitCollector) : Unit() {
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
            StatisticsYangInfo.getInstance()
        ).plus(getSpecificYangSchemas())

    override fun getUnderlayYangSchemas() = UNDERLAY_SCHEMAS
        .plus(getSpecificUnderlayYangSchemas())

    override fun getRpcs(underlayAccess: UnderlayAccess) = emptySet<RpcService<*, *>>()

    override fun provideHandlers(
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
        provideSpecificReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
        provideSpecificWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.addNoop(IIDs.IN_INTERFACE)
        wRegistry.add(GenericWriter(IIDs.IN_IN_CONFIG, InterfaceConfigWriter(underlayAccess)))

        wRegistry.addNoop(IIDs.IN_IN_SU_SUBINTERFACE)
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_SU_SU_CONFIG, SubinterfaceConfigWriter(underlayAccess)),
                IIDs.IN_IN_CONFIG)
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_AUG_IFDAMPAUG_DA_CONFIG,
            InterfaceDampeningConfigWriter(underlayAccess)),
            IIDs.IN_IN_CONFIG)
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_AUG_IFCISCOSTATSAUG_ST_CONFIG,
            InterfaceStatisticsConfigWriter(underlayAccess)),
            IIDs.IN_IN_CONFIG)
        wRegistry.addAfter(GenericWriter(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VL_CONFIG,
            SubinterfaceVlanConfigWriter(underlayAccess)), IIDs.IN_IN_SU_SU_CONFIG)

        wRegistry.add(GenericWriter(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_ADDRESS, Ipv4AddressWriter()))
        wRegistry.addAfter(GenericWriter(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_AD_CONFIG,
            Ipv4AddressConfigWriter(underlayAccess)), NetworkInstanceIIDs.NE_NE_IN_IN_CONFIG)
        wRegistry.addAfter(GenericWriter(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_CONFIG,
            Ipv4MtuConfigWriter(underlayAccess)),
            setOf(IIDs.IN_IN_SU_SU_CONFIG, IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IPV4))
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOSTATSAUG_ST_CONFIG,
            SubinterfaceStatisticsConfigWriter(underlayAccess)),
            IIDs.IN_IN_SU_SU_CONFIG)
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.IN_INTERFACE, InterfaceReader(underlayAccess)))
        rRegistry.add(GenericOperReader(IIDs.IN_IN_STATE, InterfaceStateReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_CONFIG, InterfaceConfigReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_IFCISCOSTATSAUG, IfCiscoStatsAugBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_IFCISCOSTATSAUG_STATISTICS, StatisticsBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_AUG_IFCISCOSTATSAUG_ST_CONFIG,
            InterfaceStatisticsConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_IFDAMPAUG, IfDampAugBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_AUG_IFDAMPAUG_DAMPING, DampingBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_AUG_IFDAMPAUG_DA_CONFIG,
            InterfaceDampeningConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IIDs.IN_IN_SUBINTERFACES, SubinterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.IN_IN_SU_SUBINTERFACE, SubinterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_SU_SU_CONFIG, SubinterfaceConfigReader(underlayAccess)))
        rRegistry.add(GenericOperReader(IIDs.IN_IN_SU_SU_STATE, SubinterfaceStateReader(underlayAccess)))

        rRegistry.addStructuralReader(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1, VlanAugBuilder::class.java)
        rRegistry.addStructuralReader(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VLAN, VlanBuilder::class.java)
        rRegistry.add(GenericConfigReader(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VL_CONFIG,
            SubinterfaceVlanConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1, Subinterface1Builder::class.java)
        rRegistry.addStructuralReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IPV4, Ipv4Builder::class.java)
        rRegistry.addStructuralReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_ADDRESSES, AddressesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_ADDRESS,
            Ipv4AddressReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_AD_CONFIG,
            Ipv4AddressConfigReader(underlayAccess)))

        rRegistry.add(GenericConfigReader(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_CONFIG,
                Ipv4MtuConfigReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOSTATSAUG,
                IfSubifCiscoStatsAugBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOSTATSAUG_STATISTICS,
                StatisticsBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_SU_SU_AUG_IFSUBIFCISCOSTATSAUG_ST_CONFIG,
                SubinterfaceStatisticsConfigReader(underlayAccess)))
    }

    abstract fun getSpecificYangSchemas(): Set<YangModuleInfo>
    abstract fun getSpecificUnderlayYangSchemas(): Set<YangModuleInfo>
    abstract fun provideSpecificWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, underlayAccess: UnderlayAccess)
    abstract fun provideSpecificReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess)

    companion object {
        private val UNDERLAY_SCHEMAS = setOf(
                UnderlayInterfacesYangInfo.getInstance(),
                UnderlayInterfacesOperYangInfo.getInstance(),
                UnderlayIpv4YangInfo.getInstance(),
                UnderlayInfraStatsdInfo.getInstance(),
                UnderlayTypesYangInfo.getInstance()
        )
    }
}