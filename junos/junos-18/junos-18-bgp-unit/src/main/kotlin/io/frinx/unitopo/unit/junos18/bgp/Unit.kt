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

package io.frinx.unitopo.unit.junos18.bgp

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yangtools.yang.binding.YangModuleInfo

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = emptySet()

    override fun getYangSchemas(): MutableSet<YangModuleInfo> = mutableSetOf()

    override fun provideHandlers(
        rRegistry: ModifiableReaderRegistryBuilder,
        wRegistry: ModifiableWriterRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
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

    /**
     * Following handlers are used by network-instance-unit so we don't need to register them here.
     *  - io.finx.unitipo.unit.junos18.bgp.handler.BgpProtocolConfigWriter
     *  - io.frinx.unitopo.unit.junos18.bgp.handler.aggregate.BgpAggregateConfigWriter
     */
    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        // NOP
    }

    /**
     * Following handlers are used by network-instance-unit so we don't need to register them here.
     *  - io.finx.unitipo.unit.junos18.bgp.handler.BgpProtocolReader
     *  - io.frinx.unitopo.unit.junos18.bgp.handler.aggregate.BgpAggregateReader/BgpAggregateConfigReader
     */
    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        // NOP
    }

    override fun toString(): String = "Junos 18.2 BGP translate unit"
}