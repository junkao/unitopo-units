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
package io.frinx.unitopo.unit.junos17.bgp

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpGlobalConfigReader
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpGlobalConfigWriter
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpNeighborConfigReader
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpNeighborConfigWriter
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpNeighborListReader
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.`$YangModuleInfoImpl` as OpenconfigBgp
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.`$YangModuleInfoImpl` as OpenconfigNetworkInstances
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.`$YangModuleInfoImpl` as JunosYangInfo

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    override fun getUnderlayYangSchemas(): MutableSet<YangModuleInfo> = setOf(
            JunosYangInfo.getInstance()
    ).toMutableSet()

    override fun getYangSchemas() = setOf(
        OpenconfigBgp.getInstance(),
        OpenconfigNetworkInstances.getInstance()
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

    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getRpcs(underlayAccess: UnderlayAccess) = emptySet<RpcService<*, *>>()

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_BGP)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_BG_GLOBAL)
        wRegistry.addAfter(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, BgpGlobalConfigWriter(underlayAccess),
            IIDs.NE_NE_PR_PR_BG_GLOBAL)

        wRegistry.addNoop(IIDs.NE_NE_PR_PR_BG_NEIGHBORS)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_BG_NE_NEIGHBOR)
        wRegistry.add(IIDs.NE_NE_PR_PR_BG_NE_NE_CONFIG, BgpNeighborConfigWriter(underlayAccess))
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, BgpGlobalConfigReader(underlayAccess))

        rRegistry.add(IIDs.NE_NE_PR_PR_BG_NE_NEIGHBOR, BgpNeighborListReader(underlayAccess))
        rRegistry.add(IIDs.NE_NE_PR_PR_BG_NE_NE_CONFIG, BgpNeighborConfigReader(underlayAccess))
    }

    override fun toString(): String = "Junos 17.3 BGP translate unit"
}