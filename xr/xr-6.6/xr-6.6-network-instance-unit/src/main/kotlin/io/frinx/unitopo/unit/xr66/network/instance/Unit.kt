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

package io.frinx.unitopo.unit.xr66.network.instance

import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.unitopo.ni.base.handler.vrf.ifc.VrfInterfaceConfigReader
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr66.network.instance.handler.pf.PolicyForwardingInterfaceConfigReader
import io.frinx.unitopo.unit.xr66.network.instance.handler.pf.PolicyForwardingInterfaceConfigWriter
import io.frinx.unitopo.unit.xr66.network.instance.handler.pf.PolicyForwardingInterfaceReader
import io.frinx.unitopo.unit.xr66.network.instance.handler.vrf.ifc.VrfInterfaceConfigWriter
import io.frinx.unitopo.unit.xr66.network.instance.handler.vrf.ifc.VrfInterfaceReader
import io.frinx.unitopo.unit.xr66.network.instance.handler.vrf.protocol.ProtocolConfigWriter
import io.frinx.unitopo.unit.xr66.network.instance.handler.vrf.protocol.ProtocolReader
import io.frinx.unitopo.unit.xr66.network.instance.handler.vrf.protocol.aggregate.LocalAggregateConfigReader
import io.frinx.unitopo.unit.xr66.network.instance.handler.vrf.protocol.aggregate.LocalAggregateConfigWriter
import io.frinx.unitopo.unit.xr66.network.instance.handler.vrf.protocol.aggregate.LocalAggregateReader
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import io.frinx.openconfig.openconfig.interfaces.IIDs as interfaces_IIDs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev180615.`$YangModuleInfoImpl` as UnderlayVRFYangInto
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev180227.`$YangModuleInfoImpl` as UnderlayQosMaCfgYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.`$YangModuleInfoImpl` as BgpExtensionYangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.`$YangModuleInfoImpl` as BgpYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.`$YangModuleInfoImpl` as LocalRoutingYangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.cisco.rev171109.`$YangModuleInfoImpl` as PfNwYangModuleInfoImpl
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.`$YangModuleInfoImpl` as NetworkInstanceYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.`$YangModuleInfoImpl` as PfYangModuleInfoImpl

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {

    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

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

    override fun provideHandlers(
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        rRegistry.addCheckRegistry(ChecksMap.OPENCONFIG_REGISTRY)
        wRegistry.addCheckRegistry(ChecksMap.OPENCONFIG_REGISTRY)
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, underlay: UnderlayAccess) {
        wRegistry.addNoop(IIDs.NE_NETWORKINSTANCE)
        wRegistry.addNoop(IIDs.NE_NE_PR_PROTOCOL)
        wRegistry.add(IIDs.NE_NE_PR_PR_CONFIG, ProtocolConfigWriter(underlay))
        wRegistry.addNoop(IIDs.NE_NE_CONFIG)
        wRegistry.addNoop(IIDs.NE_NE_INTERINSTANCEPOLICIES)
        wRegistry.addNoop(IIDs.NE_NE_IN_APPLYPOLICY)
        wRegistry.addNoop(IIDs.NE_NE_IN_AP_CONFIG)
        wRegistry.addNoop(IIDs.NE_NE_IN_INTERFACE)
        wRegistry.addAfter(IIDs.NE_NE_IN_IN_CONFIG, VrfInterfaceConfigWriter(underlay),
            IIDs.NE_NE_CONFIG)
        // PF
        wRegistry.addNoop(IIDs.NE_NE_PO_IN_INTERFACE)
        wRegistry.subtreeAddAfter(IIDs.NE_NE_PO_IN_IN_CONFIG, PolicyForwardingInterfaceConfigWriter(underlay),
            setOf(RWUtils.cutIdFromStart(IIDs.NE_NE_PO_IN_IN_CO_AUG_NIPFIFCISCOAUG,
                IIDs.NE_TO_NO_CO_AUG_CONFIGURATION1_NE_NE_PO_IN_IN_CONFIG)
            ), interfaces_IIDs.IN_IN_CONFIG)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_LO_AGGREGATE)
        wRegistry.subtreeAdd(IIDs.NE_NE_PR_PR_LO_AG_CONFIG, LocalAggregateConfigWriter(underlay),
            setOf(RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_LO_AG_CO_AUG_NIPROTAGGAUG, IIDs.NE_NE_PR_PR_LO_AG_CONFIG),
                IIDs.NE_NE_CONFIG, IIDs.NE_NE_PR_PR_BG_GL_CONFIG, IIDs.NE_NE_PR_PR_OS_GL_CONFIG,
                IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_CONFIG)
        )
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlay: UnderlayAccess) {
        rRegistry.add(IIDs.NE_NETWORKINSTANCE, NetworkInstanceReader(underlay))
        rRegistry.add(IIDs.NE_NE_CONFIG, NetworkInstanceConfigReader(underlay))
        rRegistry.add(IIDs.NE_NE_IN_INTERFACE, VrfInterfaceReader(underlay))
        rRegistry.add(IIDs.NE_NE_IN_IN_CONFIG, VrfInterfaceConfigReader())
        // PF
        rRegistry.add(IIDs.NE_NE_PO_IN_INTERFACE, PolicyForwardingInterfaceReader(underlay))
        rRegistry.add(IIDs.NE_NE_PO_IN_IN_CONFIG, PolicyForwardingInterfaceConfigReader(underlay))
        rRegistry.add(IIDs.NE_NE_PR_PROTOCOL, ProtocolReader(underlay))
        rRegistry.add(IIDs.NE_NE_PR_PR_LO_AGGREGATE, LocalAggregateReader(underlay))
        rRegistry.subtreeAdd(IIDs.NE_NE_PR_PR_LO_AG_CONFIG, LocalAggregateConfigReader(underlay),
            setOf(RWUtils.cutIdFromStart(
                IIDs.NE_NE_PR_PR_LO_AG_CO_AUG_NIPROTAGGAUG, IIDs.NE_NE_PR_PR_LO_AG_CONFIG))
            )
    }

    override fun toString(): String = "Cisco-IOS-XR-ifmgr-cfg@2017-09-07 VRF of interface translate unit"
}