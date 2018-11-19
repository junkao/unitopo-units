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
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.network.instance.NetworkInstanceUnit
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config as NetworkInstanceConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder as NetworkInstanceConfigBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.root.rev180101.`$YangModuleInfoImpl` as UnderlayConfRootYangModuleInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.`$YangModuleInfoImpl` as UnderlayRoutingInstanceYangModuleInfo

class Unit(private val registry: TranslationUnitCollector) : NetworkInstanceUnit() {

    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
        UnderlayConfRootYangModuleInfo.getInstance(),
        UnderlayRoutingInstanceYangModuleInfo.getInstance()
    )

    override fun provideSpecificWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_CONFIG, NetworkInstanceConfigWriter(underlayAccess)),
                /*handle after ifc configuration*/ io.frinx.openconfig.openconfig.interfaces.IIDs.IN_IN_CONFIG)
    }

    override fun provideSpecificReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.add(GenericConfigListReader(IIDs.NE_NETWORKINSTANCE, NetworkInstanceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader<NetworkInstanceConfig, NetworkInstanceConfigBuilder>(
            IIDs.NE_NE_CONFIG, NetworkInstanceConfigReader(underlayAccess)))
    }

    override fun toString(): String = "Junos 18.2 network-instance translate unit"
}