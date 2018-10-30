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

package io.frinx.unitopo.unit.junos18.configmetadata

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericOperReader
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.configuration.metadata.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.configmetadata.handler.ConfigMetadataReader
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.configuration.metadata.rev180731.`$YangModuleInfoImpl` as OpenconfigConfigMetadata
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.rpc.system.rev180101.`$YangModuleInfoImpl` as JunosRpcSystemYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.common.types.rev180101.`$YangModuleInfoImpl` as JunosCommonTypesYangInfo
class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {

    override fun getUnderlayYangSchemas(): MutableSet<YangModuleInfo> = setOf(
            JunosRpcSystemYangInfo.getInstance(),
            JunosCommonTypesYangInfo.getInstance()
    ).toMutableSet()

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(
            OpenconfigConfigMetadata.getInstance()
    )

    override fun provideHandlers(
        rRegistry: ModifiableReaderRegistryBuilder,
        wRegistry: ModifiableWriterRegistryBuilder,
        context: UnderlayAccess
    ) =
            provideReaders(rRegistry, context)

    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getRpcs(context: UnderlayAccess) = emptySet<RpcService<*, *>>()

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.add(GenericOperReader(IIDs.CONFIGURATIONMETADATA, ConfigMetadataReader(underlayAccess)))
    }

    override fun toString() = "Junos 18.2 Configuration metadata translation unit"
}