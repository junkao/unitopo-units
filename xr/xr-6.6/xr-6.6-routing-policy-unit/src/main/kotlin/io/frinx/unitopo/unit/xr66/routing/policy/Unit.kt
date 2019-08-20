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

package io.frinx.unitopo.unit.xr66.routing.policy

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.policy.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr66.routing.policy.handlers.policy.PolicyDefinitionConfigReader
import io.frinx.unitopo.unit.xr66.routing.policy.handlers.policy.PolicyDefinitionConfigWriter
import io.frinx.unitopo.unit.xr66.routing.policy.handlers.policy.PolicyDefinitionReader
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev170907.`$YangModuleInfoImpl` as UnderlayRoutingPolicyYangModule
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.`$YangModuleInfoImpl` as UnderlayTypesYangModule

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        if (reg != null) {
            reg!!.close()
        }
    }

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(IIDs.FRINX_OPENCONFIG_ROUTING_POLICY)

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
        UnderlayRoutingPolicyYangModule.getInstance(),
        UnderlayTypesYangModule.getInstance())

    override fun getRpcs(context: UnderlayAccess) = emptySet<RpcService<out DataObject, out DataObject>>()

    override fun provideHandlers(
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        access: UnderlayAccess
    ) {
        provideReaders(rRegistry, access)
        provideWriters(wRegistry, access)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, access: UnderlayAccess) {
        wRegistry.addNoop(IIDs.ROUTINGPOLICY)
        wRegistry.addNoop(IIDs.RO_POLICYDEFINITIONS)
        wRegistry.addNoop(IIDs.RO_PO_POLICYDEFINITION)
        wRegistry.add(IIDs.RO_PO_PO_CONFIG, PolicyDefinitionConfigWriter(access))
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, access: UnderlayAccess) {
        rRegistry.add(IIDs.RO_PO_POLICYDEFINITION, PolicyDefinitionReader(access))
        rRegistry.add(IIDs.RO_PO_PO_CONFIG, PolicyDefinitionConfigReader(access))
    }

    override fun toString() = "XR 6.6 routing policy translate unit"
}