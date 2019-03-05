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
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.handlers.network.instance.NetworkInstanceUnit
import io.frinx.unitopo.unit.utils.NoopWriter
import io.frinx.unitopo.unit.xr623.network.instance.vrf.ifc.VrfInterfaceConfigReader
import io.frinx.unitopo.unit.xr623.network.instance.vrf.protocol.ProtocolConfigWriter
import io.frinx.unitopo.unit.xr623.network.instance.vrf.ifc.VrfInterfaceConfigWriter
import io.frinx.unitopo.unit.xr623.network.instance.vrf.ifc.VrfInterfaceReader
import io.frinx.unitopo.unit.xr623.network.instance.vrf.protocol.ProtocolReader
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev161219.`$YangModuleInfoImpl` as UnderlayVRFYangInto

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
            UnderlayVRFYangInto.getInstance()
    )

    override fun provideSpecificWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.add(GenericWriter(IIDs.NE_NE_CONFIG, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_IN_INTERFACE, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_IN_IN_CONFIG, VrfInterfaceConfigWriter(underlayAccess)),
            IIDs.NE_NE_CONFIG)
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_PR_PR_CONFIG, ProtocolConfigWriter(underlayAccess)),
            IIDs.NE_NE_CONFIG)
    }

    override fun provideSpecificReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.add(GenericConfigListReader(IIDs.NE_NETWORKINSTANCE, NetworkInstanceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_CONFIG, NetworkInstanceConfigReader()))
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PROTOCOL, ProtocolReader(underlayAccess)))
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_IN_INTERFACE, VrfInterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_IN_IN_CONFIG, VrfInterfaceConfigReader()))
    }

    override fun toString(): String = "XR 6 (2016-12-19) network-instance translate unit"

    override fun useAutoCommit() = false
}