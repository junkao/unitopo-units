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

package io.frinx.unitopo.unit.junos18.network.instance

import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.interfaces.IIDs as InterfaceIIDs
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.unitopo.ni.base.handler.vrf.protocol.ProtocolConfigReader
import io.frinx.unitopo.ni.base.handler.vrf.protocol.ProtocolStateReader
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.network.instance.handler.NetworkInstanceConfigReader
import io.frinx.unitopo.unit.junos18.network.instance.handler.NetworkInstanceConfigWriter
import io.frinx.unitopo.unit.junos18.network.instance.handler.NetworkInstanceReader
import io.frinx.unitopo.unit.junos18.network.instance.handler.vrf.ifc.VrfInterfaceConfigReader
import io.frinx.unitopo.unit.junos18.network.instance.handler.vrf.ifc.InterfaceConfigWriter
import io.frinx.unitopo.unit.junos18.network.instance.handler.vrf.ifc.VrfInterfaceReader
import io.frinx.unitopo.unit.junos18.network.instance.handler.vrf.protocol.ProtocolConfigWriter
import io.frinx.unitopo.unit.junos18.network.instance.handler.vrf.protocol.ProtocolReader
import io.frinx.unitopo.unit.junos18.network.instance.handler.vrf.protocol.aggregate.AggregateConfigReader
import io.frinx.unitopo.unit.junos18.network.instance.handler.vrf.protocol.aggregate.AggregateConfigWriter
import io.frinx.unitopo.unit.junos18.network.instance.handler.vrf.protocol.aggregate.AggregateReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.NiProtAggAug
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.`$YangModuleInfoImpl` as OpenconfigBgpExtensionYangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.`$YangModuleInfoImpl` as OpenconfigLocalRoutingYangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.Config as AggregateConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.`$YangModuleInfoImpl` as NetInstanceYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.`$YangModuleInfoImpl` as OpenconfigPolicyTypesYangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.`$YangModuleInfoImpl` as OpenconfigInetTypesYangModuleInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.root.rev180101.`$YangModuleInfoImpl` as UnderlayConfRootYangModuleInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.`$YangModuleInfoImpl` as UnderlayRoutingInstanceYangModuleInfo

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {

    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas(): Set<YangModuleInfo> =
        setOf(
            NetInstanceYangInfo.getInstance(),
            OpenconfigBgpExtensionYangModuleInfo.getInstance(),
            OpenconfigInetTypesYangModuleInfo.getInstance(),
            OpenconfigLocalRoutingYangModuleInfo.getInstance(),
            OpenconfigPolicyTypesYangModuleInfo.getInstance()
        )

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
        UnderlayConfRootYangModuleInfo.getInstance(),
        UnderlayRoutingInstanceYangModuleInfo.getInstance()
    )

    override fun provideHandlers(
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        val checkRegistry = ChecksMap.getOpenconfigCheckRegistry()
        rRegistry.setCheckRegistry(checkRegistry)
        provideReaders(rRegistry, underlayAccess)
        wRegistry.setCheckRegistry(checkRegistry)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, underlay: UnderlayAccess) {
        wRegistry.addNoop(IIDs.NE_NETWORKINSTANCE)
        wRegistry.addAfter(IIDs.NE_NE_CONFIG, NetworkInstanceConfigWriter(underlay),
            /*handle after ifc configuration*/ InterfaceIIDs.IN_IN_CONFIG)

        wRegistry.addNoop(IIDs.NE_NE_IN_INTERFACE)
        wRegistry.add(IIDs.NE_NE_IN_IN_CONFIG, InterfaceConfigWriter(underlay))

        wRegistry.addNoop(IIDs.NE_NE_PR_PROTOCOL)
        wRegistry.add(IIDs.NE_NE_PR_PR_CONFIG, ProtocolConfigWriter(underlay))

        wRegistry.addNoop(IIDs.NE_NE_PR_PR_LO_AGGREGATE)
        wRegistry.subtreeAdd(IIDs.NE_NE_PR_PR_LO_AG_CONFIG, AggregateConfigWriter(underlay),
            NE_NE_PR_PR_LO_AG_CONFIG_SUBTREE)
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlay: UnderlayAccess) {
        rRegistry.add(IIDs.NE_NETWORKINSTANCE, NetworkInstanceReader(underlay))
        rRegistry.add(IIDs.NE_NE_CONFIG, NetworkInstanceConfigReader(underlay))

        rRegistry.add(IIDs.NE_NE_IN_INTERFACE, VrfInterfaceReader(underlay))
        rRegistry.add(IIDs.NE_NE_IN_IN_CONFIG, VrfInterfaceConfigReader(underlay))

        rRegistry.add(IIDs.NE_NE_PR_PROTOCOL, ProtocolReader(underlay))
        rRegistry.add(IIDs.NE_NE_PR_PR_CONFIG, ProtocolConfigReader())
        rRegistry.add(IIDs.NE_NE_PR_PR_STATE, ProtocolStateReader())
        rRegistry.add(IIDs.NE_NE_PR_PR_LO_AGGREGATE, AggregateReader(underlay))
        rRegistry.subtreeAdd(IIDs.NE_NE_PR_PR_LO_AG_CONFIG, AggregateConfigReader(underlay),
            NE_NE_PR_PR_LO_AG_CONFIG_SUBTREE)
    }

    override fun toString(): String = "Junos 18.2 network-instance translate unit"

    companion object {
        private val NE_NE_PR_PR_LO_AG_CONFIG_SUBTREE = setOf(
            InstanceIdentifier.create(AggregateConfig::class.java).augmentation(NiProtAggAug::class.java)
        )
    }
}