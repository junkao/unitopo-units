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
package io.frinx.unitopo.unit.junos17.network.instance

import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.unitopo.ni.base.handler.vrf.protocol.ProtocolConfigReader
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.network.instance.handler.NetworkInstanceConfigReader
import io.frinx.unitopo.unit.junos17.network.instance.handler.NetworkInstanceConfigWriter
import io.frinx.unitopo.unit.junos17.network.instance.handler.NetworkInstanceReader
import io.frinx.unitopo.unit.junos17.network.instance.handler.NetworkInstanceStateReader
import io.frinx.unitopo.unit.junos17.network.instance.handler.vrf.protocol.ProtocolReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.`$YangModuleInfoImpl`
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.`$YangModuleInfoImpl` as JunosYangInfo

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {

    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(
        `$YangModuleInfoImpl`.getInstance()
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

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
        JunosYangInfo.getInstance()
    )

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.addNoop(IIDs.NE_NETWORKINSTANCE)
        wRegistry.addNoop(IIDs.NE_NE_PR_PROTOCOL)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_CONFIG)

        wRegistry.addAfter(IIDs.NE_NE_CONFIG, NetworkInstanceConfigWriter(underlayAccess),
                /*handle after ifc configuration*/ io.frinx.openconfig.openconfig.interfaces.IIDs.IN_IN_CONFIG)
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.add(IIDs.NE_NETWORKINSTANCE, NetworkInstanceReader(underlayAccess))
        rRegistry.add(IIDs.NE_NE_CONFIG, NetworkInstanceConfigReader(underlayAccess))
        rRegistry.add(IIDs.NE_NE_STATE, NetworkInstanceStateReader(underlayAccess))

        rRegistry.add(IIDs.NE_NE_PR_PROTOCOL, ProtocolReader(underlayAccess))
        rRegistry.add(IIDs.NE_NE_PR_PR_CONFIG, ProtocolConfigReader())
    }

    override fun toString(): String = "Junos 17.3 network-instance translate unit"
}