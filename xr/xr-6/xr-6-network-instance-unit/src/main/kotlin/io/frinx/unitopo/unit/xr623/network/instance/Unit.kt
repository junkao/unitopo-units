/*
 * Copyright © 2019 Frinx and others.
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

package io.frinx.unitopo.unit.xr623.network.instance

import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.frinx.openconfig.openconfig.interfaces.IIDs as InterfacesIIDs
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.handlers.network.instance.NetworkInstanceUnit
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NoopWriter
import io.frinx.unitopo.unit.xr623.network.instance.policy.forwarding.PolicyForwardingInterfaceConfigReader
import io.frinx.unitopo.unit.xr623.network.instance.policy.forwarding.PolicyForwardingInterfaceConfigWriter
import io.frinx.unitopo.unit.xr623.network.instance.policy.forwarding.PolicyForwardingInterfaceReader
import io.frinx.unitopo.unit.xr623.network.instance.vrf.ifc.VrfInterfaceConfigReader
import io.frinx.unitopo.unit.xr623.network.instance.vrf.ifc.VrfInterfaceConfigWriter
import io.frinx.unitopo.unit.xr623.network.instance.vrf.ifc.VrfInterfaceReader
import io.frinx.unitopo.unit.xr623.network.instance.vrf.protocol.ProtocolConfigWriter
import io.frinx.unitopo.unit.xr623.network.instance.vrf.protocol.ProtocolReader
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.`$YangModuleInfoImpl` as UnderlayIsisYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.`$YangModuleInfoImpl` as UnderlayIsisTypesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.`$YangModuleInfoImpl` as UnderlayTypesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev161219.`$YangModuleInfoImpl` as UnderlayVRFYangInto
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev161223.`$YangModuleInfoImpl` as UnderlayQosMaCfgYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.`$YangModuleInfoImpl` as PfYangModuleInfoImpl
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.cisco.rev171109.`$YangModuleInfoImpl` as PfNwYangModuleInfoImpl
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.`$YangModuleInfoImpl` as NetworkInstanceYangInfo

open class Unit(private val registry: TranslationUnitCollector) : NetworkInstanceUnit() {

    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
            UnderlayInterfacesYangInfo.getInstance(),
            UnderlayIsisYangInfo.getInstance(),
            UnderlayIsisTypesYangInfo.getInstance(),
            UnderlayTypesYangInfo.getInstance(),
            UnderlayVRFYangInto.getInstance(),
            UnderlayQosMaCfgYangInfo.getInstance()
    )

    override fun getYangSchemas() = setOf(
            PfYangModuleInfoImpl.getInstance(),
            PfNwYangModuleInfoImpl.getInstance(),
            NetworkInstanceYangInfo.getInstance()
    )

    override fun provideSpecificWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, underlay: UnderlayAccess) {
        wRegistry.add(GenericWriter(IIDs.NE_NE_CONFIG, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_IN_INTERFACE, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_IN_IN_CONFIG, VrfInterfaceConfigWriter(underlay)),
            IIDs.NE_NE_CONFIG)
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_PR_PR_CONFIG, ProtocolConfigWriter(underlay)),
            IIDs.NE_NE_CONFIG)
        // PF
        wRegistry.addNoop(IIDs.NE_NE_PO_IN_INTERFACE)
        wRegistry.subtreeAddAfter(
            setOf(
                RWUtils.cutIdFromStart(IIDs.NE_NE_PO_IN_IN_CO_AUG_NIPFIFCISCOAUG,
                        IIDs.NE_TO_NO_CO_AUG_CONFIGURATION1_NE_NE_PO_IN_IN_CONFIG)),
                GenericWriter(IIDs.NE_NE_PO_IN_IN_CONFIG, PolicyForwardingInterfaceConfigWriter(underlay)),
            InterfacesIIDs.IN_IN_CONFIG
        )
    }

    override fun provideSpecificReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlay: UnderlayAccess) {
        rRegistry.add(GenericConfigListReader(IIDs.NE_NETWORKINSTANCE, NetworkInstanceReader(underlay)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_CONFIG, NetworkInstanceConfigReader()))
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PROTOCOL, ProtocolReader(underlay)))
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_IN_INTERFACE, VrfInterfaceReader(underlay)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_IN_IN_CONFIG, VrfInterfaceConfigReader()))

        // PF
        rRegistry.add(IIDs.NE_NE_PO_IN_INTERFACE, PolicyForwardingInterfaceReader(underlay))
        rRegistry.add(IIDs.NE_NE_PO_IN_IN_CONFIG, PolicyForwardingInterfaceConfigReader(underlay))
    }

    override fun toString(): String = "XR 6 (2016-12-19) network-instance translate unit"
}