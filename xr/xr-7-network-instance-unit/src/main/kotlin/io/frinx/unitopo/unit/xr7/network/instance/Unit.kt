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
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.network.instance.NetworkInstanceUnit
import io.frinx.unitopo.unit.utils.NoopWriter
import io.frinx.unitopo.unit.xr7.network.instance.vrf.ifc.VrfInterfaceConfigReader
import io.frinx.unitopo.unit.xr7.network.instance.vrf.ifc.VrfInterfaceConfigWriter
import io.frinx.unitopo.unit.xr7.network.instance.vrf.ifc.VrfInterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo

class Unit(private val registry: TranslationUnitCollector) : NetworkInstanceUnit() {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
            UnderlayInterfacesYangInfo.getInstance()
    )

    override fun provideSpecificWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.add(GenericWriter(IIDs.NE_NE_CONFIG, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_INTERINSTANCEPOLICIES, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_IN_APPLYPOLICY, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_IN_AP_CONFIG, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_IN_INTERFACE, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_IN_IN_CONFIG, VrfInterfaceConfigWriter(underlayAccess)),
                IIDs.NE_NE_CONFIG)
    }

    override fun provideSpecificReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.add(GenericConfigListReader(IIDs.NE_NETWORKINSTANCE, NetworkInstanceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader<Config, ConfigBuilder>(IIDs.NE_NE_CONFIG,
            NetworkInstanceConfigReader(underlayAccess)))
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_IN_INTERFACE, VrfInterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_IN_IN_CONFIG, VrfInterfaceConfigReader()))
    }

    override fun toString(): String = "Cisco-IOS-XR-ifmgr-cfg@2017-09-07 VRF of interface translate unit"
}