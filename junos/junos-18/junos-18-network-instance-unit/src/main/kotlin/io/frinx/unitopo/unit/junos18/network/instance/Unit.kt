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

import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.network.instance.vrf.ifc.InterfaceConfigReader
import io.frinx.unitopo.unit.junos18.network.instance.vrf.ifc.InterfaceConfigWriter
import io.frinx.unitopo.unit.junos18.network.instance.vrf.ifc.InterfaceReader
import io.frinx.unitopo.handlers.network.instance.NetworkInstanceUnit
import io.frinx.unitopo.unit.junos18.network.instance.vrf.protocol.ProtocolConfigWriter
import io.frinx.unitopo.unit.junos18.network.instance.vrf.protocol.ProtocolReader
import io.frinx.unitopo.unit.junos18.network.instance.vrf.protocol.aggregate.AggregateConfigReader
import io.frinx.unitopo.unit.junos18.network.instance.vrf.protocol.aggregate.AggregateConfigWriter
import io.frinx.unitopo.unit.junos18.network.instance.vrf.protocol.aggregate.AggregateReader
import io.frinx.unitopo.unit.utils.NoopListWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.NiProtAggAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.LocalAggregatesBuilder
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.Config as AggregateConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config as NetworkInstanceConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder as NetworkInstanceConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.`$YangModuleInfoImpl` as OpenconfigBgpExtensionYangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.`$YangModuleInfoImpl` as OpenconfigInetTypesYangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.`$YangModuleInfoImpl` as OpenconfigLocalRoutingYangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.`$YangModuleInfoImpl` as OpenconfigPolicyTypesYangModuleInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.root.rev180101.`$YangModuleInfoImpl` as UnderlayConfRootYangModuleInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.`$YangModuleInfoImpl` as UnderlayRoutingInstanceYangModuleInfo
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class Unit(private val registry: TranslationUnitCollector) : NetworkInstanceUnit() {

    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas(): Set<YangModuleInfo> =
        setOf(
            OpenconfigBgpExtensionYangModuleInfo.getInstance(),
            OpenconfigInetTypesYangModuleInfo.getInstance(),
            OpenconfigLocalRoutingYangModuleInfo.getInstance(),
            OpenconfigPolicyTypesYangModuleInfo.getInstance()
        ).plus(super.getYangSchemas())

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
        UnderlayConfRootYangModuleInfo.getInstance(),
        UnderlayRoutingInstanceYangModuleInfo.getInstance()
    )

    override fun provideSpecificWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_CONFIG, NetworkInstanceConfigWriter(underlayAccess)),
            /*handle after ifc configuration*/ io.frinx.openconfig.openconfig.interfaces.IIDs.IN_IN_CONFIG)

        wRegistry.add(GenericListWriter(IIDs.NE_NE_IN_INTERFACE, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_IN_IN_CONFIG, InterfaceConfigWriter(underlayAccess)))

        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_CONFIG, ProtocolConfigWriter(underlayAccess)))

        wRegistry.add(GenericListWriter(IIDs.NE_NE_PR_PR_LO_AGGREGATE, NoopListWriter()))
        wRegistry.subtreeAdd(NE_NE_PR_PR_LO_AG_CONFIG_SUBTREE,
            GenericWriter(IIDs.NE_NE_PR_PR_LO_AG_CONFIG, AggregateConfigWriter(underlayAccess)))
    }

    override fun provideSpecificReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.add(GenericConfigListReader(IIDs.NE_NETWORKINSTANCE, NetworkInstanceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader<NetworkInstanceConfig, NetworkInstanceConfigBuilder>(
            IIDs.NE_NE_CONFIG, NetworkInstanceConfigReader(underlayAccess)))

        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_IN_INTERFACE, InterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_IN_IN_CONFIG, InterfaceConfigReader(underlayAccess)))

        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PROTOCOL, ProtocolReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_LOCALAGGREGATES, LocalAggregatesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PR_LO_AGGREGATE, AggregateReader(underlayAccess)))
        rRegistry.subtreeAdd(
            NE_NE_PR_PR_LO_AG_CONFIG_SUBTREE,
            GenericConfigReader(IIDs.NE_NE_PR_PR_LO_AG_CONFIG, AggregateConfigReader(underlayAccess)))
    }

    override fun toString(): String = "Junos 18.2 network-instance translate unit"

    companion object {
        private val NE_NE_PR_PR_LO_AG_CONFIG_ROOT = IID.create(AggregateConfig::class.java)
        private val NE_NE_PR_PR_LO_AG_CONFIG_SUBTREE = setOf(
            NE_NE_PR_PR_LO_AG_CONFIG_ROOT.augmentation(NiProtAggAug::class.java)
        )
    }
}