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

package io.frinx.unitopo.unit.xr7.network.instance

import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.handlers.network.instance.NetworkInstanceUnit
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import io.frinx.unitopo.unit.xr7.network.instance.vrf.protocol.ProtocolConfigWriter
import io.frinx.unitopo.unit.xr7.network.instance.policy.forwarding.PolicyForwardingInterfaceConfigReader
import io.frinx.unitopo.unit.xr7.network.instance.policy.forwarding.PolicyForwardingInterfaceConfigWriter
import io.frinx.unitopo.unit.xr7.network.instance.policy.forwarding.PolicyForwardingInterfaceReader
import io.frinx.unitopo.unit.xr7.network.instance.vrf.protocol.ProtocolReader
import io.frinx.unitopo.unit.xr7.network.instance.vrf.ifc.VrfInterfaceConfigReader
import io.frinx.unitopo.unit.xr7.network.instance.vrf.ifc.VrfInterfaceConfigWriter
import io.frinx.unitopo.unit.xr7.network.instance.vrf.ifc.VrfInterfaceReader
import io.frinx.unitopo.unit.xr7.network.instance.vrf.protocol.aggregate.AggregateConfigReader
import io.frinx.unitopo.unit.xr7.network.instance.vrf.protocol.aggregate.AggregateConfigWriter
import io.frinx.unitopo.unit.xr7.network.instance.vrf.protocol.aggregate.AggregateReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.LocalAggregatesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.policy.forwarding.top.PolicyForwardingBuilder
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import io.frinx.openconfig.openconfig.interfaces.IIDs as interfaces_IIDs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev180227.`$YangModuleInfoImpl` as UnderlayQosMaCfgYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.cisco.rev171109.`$YangModuleInfoImpl` as PfNwYangModuleInfoImpl
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.`$YangModuleInfoImpl` as PfYangModuleInfoImpl
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev180615.`$YangModuleInfoImpl` as UnderlayVRFYangInto
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.`$YangModuleInfoImpl` as BgpExtensionYangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.`$YangModuleInfoImpl` as BgpYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.`$YangModuleInfoImpl` as NetworkInstanceYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.`$YangModuleInfoImpl` as LocalRoutingYangModuleInfo

class Unit(private val registry: TranslationUnitCollector) : NetworkInstanceUnit() {

    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun useAutoCommit() = false

    override fun getYangSchemas() = setOf(
        PfYangModuleInfoImpl.getInstance(),
        PfNwYangModuleInfoImpl.getInstance(),
        NetworkInstanceYangModule.getInstance(),
        BgpYangModule.getInstance(),
        BgpExtensionYangModuleInfo.getInstance(),
        LocalRoutingYangModuleInfo.getInstance()
    )

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
        UnderlayInterfacesYangInfo.getInstance(),
        UnderlayQosMaCfgYangInfo.getInstance(),
        UnderlayVRFYangInto.getInstance()
    )

    override fun provideSpecificWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, underlay: UnderlayAccess) {
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_CONFIG, ProtocolConfigWriter(underlay)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_CONFIG, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_INTERINSTANCEPOLICIES, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_IN_APPLYPOLICY, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_IN_AP_CONFIG, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_IN_INTERFACE, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_IN_IN_CONFIG, VrfInterfaceConfigWriter(underlay)),
            IIDs.NE_NE_CONFIG)
        // PF
        wRegistry.add(GenericWriter(IIDs.NE_NE_PO_IN_INTERFACE, NoopWriter()))
        wRegistry.subtreeAddAfter(setOf(
            RWUtils.cutIdFromStart(IIDs.NE_NE_PO_IN_IN_CO_AUG_NIPFIFCISCOAUG,
                IIDs.NE_TO_NO_CO_AUG_CONFIGURATION1_NE_NE_PO_IN_IN_CONFIG)),
            GenericWriter(IIDs.NE_NE_PO_IN_IN_CONFIG, PolicyForwardingInterfaceConfigWriter(underlay)),
            interfaces_IIDs.IN_IN_CONFIG)
        wRegistry.add(GenericListWriter(IIDs.NE_NE_PR_PR_LO_AGGREGATE, NoopListWriter()))
        wRegistry.subtreeAdd(
            setOf(RWUtils.cutIdFromStart(
                IIDs.NE_NE_PR_PR_LO_AG_CO_AUG_NIPROTAGGAUG, IIDs.NE_NE_PR_PR_LO_AG_CONFIG)),
            GenericWriter(IIDs.NE_NE_PR_PR_LO_AG_CONFIG, AggregateConfigWriter(underlay)))
    }

    override fun provideSpecificReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlay: UnderlayAccess) {
        rRegistry.add(GenericConfigListReader(IIDs.NE_NETWORKINSTANCE, NetworkInstanceReader(underlay)))
        rRegistry.add(GenericConfigReader<Config, ConfigBuilder>(IIDs.NE_NE_CONFIG,
            NetworkInstanceConfigReader(underlay)))
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_IN_INTERFACE, VrfInterfaceReader(underlay)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_IN_IN_CONFIG, VrfInterfaceConfigReader()))
        // PF
        rRegistry.addStructuralReader(IIDs.NE_NE_POLICYFORWARDING, PolicyForwardingBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_PO_INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(
            GenericConfigListReader(IIDs.NE_NE_PO_IN_INTERFACE, PolicyForwardingInterfaceReader(underlay)))
        rRegistry.add(
            GenericConfigReader(IIDs.NE_NE_PO_IN_IN_CONFIG, PolicyForwardingInterfaceConfigReader(underlay)))
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PROTOCOL, ProtocolReader(underlay)))
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_LOCALAGGREGATES, LocalAggregatesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PR_LO_AGGREGATE, AggregateReader(underlay)))
        rRegistry.subtreeAdd(
            setOf(RWUtils.cutIdFromStart(
                IIDs.NE_NE_PR_PR_LO_AG_CO_AUG_NIPROTAGGAUG, IIDs.NE_NE_PR_PR_LO_AG_CONFIG)),
            GenericConfigReader(IIDs.NE_NE_PR_PR_LO_AG_CONFIG, AggregateConfigReader(underlay)))
    }

    override fun toString(): String = "Cisco-IOS-XR-ifmgr-cfg@2017-09-07 VRF of interface translate unit"
}