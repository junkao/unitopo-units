/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.routing.policy

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.policy.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.routing.policy.handlers.policy.PolicyDefinitionConfigReader
import io.frinx.unitopo.unit.xr7.routing.policy.handlers.policy.PolicyDefinitionConfigWriter
import io.frinx.unitopo.unit.xr7.routing.policy.handlers.policy.PolicyDefinitionReader
import io.frinx.unitopo.unit.xr7.routing.policy.handlers.policy.StatementsReader
import io.frinx.unitopo.unit.xr7.routing.policy.handlers.policy.StatementsWriter
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import io.frinx.openconfig.openconfig.bgp.IIDs as BgpIIDs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev190405.`$YangModuleInfoImpl` as UnderlayRoutingPolicyYangModule
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.`$YangModuleInfoImpl` as UnderlayTypesYangModule

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

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(
        IIDs.FRINX_OPENCONFIG_ROUTING_POLICY,
        BgpIIDs.FRINX_OPENCONFIG_BGP_POLICY)

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
        wRegistry.subtreeAddAfter(IIDs.RO_PO_PO_STATEMENTS, StatementsWriter(access), STATEMENTS_SUBTREES,
            IIDs.RO_PO_PO_CONFIG)
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, access: UnderlayAccess) {
        rRegistry.add(IIDs.RO_PO_POLICYDEFINITION, PolicyDefinitionReader(access))
        rRegistry.add(IIDs.RO_PO_PO_CONFIG, PolicyDefinitionConfigReader(access))
        rRegistry.subtreeAdd(IIDs.RO_PO_PO_STATEMENTS, StatementsReader(access), STATEMENTS_SUBTREES)
    }

    override fun toString() = "Translate unit for Cisco-IOS-XR-policy-repository-cfg@2019-04-05"

    companion object {
        private val STATEMENTS_CONDITIONS_SUBTREES: Set<InstanceIdentifier<*>> = setOf(
            IIDs.RO_PO_PO_ST_ST_CONDITIONS,
            IIDs.RO_PO_PO_ST_ST_CO_CONFIG,
            IIDs.RO_PO_PO_ST_ST_CO_MATCHPREFIXSET,
            IIDs.ROUT_POLI_POLI_STAT_STAT_COND_MATC_CONFIG,
            BgpIIDs.RO_PO_PO_ST_ST_CO_AUG_CONDITIONS2,
            BgpIIDs.RO_PO_PO_ST_ST_CO_AUG_CONDITIONS2_BGPCONDITIONS,
            BgpIIDs.RO_PO_PO_ST_ST_CO_AUG_CONDITIONS2_BG_CONFIG,

            // As path length
            BgpIIDs.RO_PO_PO_ST_ST_CO_AUG_CONDITIONS2_BG_ASPATHLENGTH,
            BgpIIDs.RO_PO_PO_ST_ST_CO_AUG_CONDITIONS2_BG_AS_CONFIG,

            // Match As path set
            BgpIIDs.RO_PO_PO_ST_ST_CO_AUG_CONDITIONS2_BG_MATCHASPATHSET,
            BgpIIDs.RO_PO_PO_ST_ST_CO_AUG_CONDITIONS2_BG_MA_CONFIG,

            // Match community set
            BgpIIDs.RO_PO_PO_ST_ST_CO_AUG_CONDITIONS2_BG_MATCHCOMMUNITYSET,
            BgpIIDs.ROU_POL_POL_STA_STA_CON_AUG_CONDITIONS2_BGP_MAT_CONFIG,

            IIDs.RO_PO_PO_ST_ST_CONFIG,
            IIDs.RO_PO_PO_ST_ST_ACTIONS,
            IIDs.RO_PO_PO_ST_ST_AC_CONFIG,
            IIDs.RO_PO_PO_ST_STATEMENT)

        private val STATEMENTS_ACTIONS_SUBTREES: Set<InstanceIdentifier<*>> = setOf(
            IIDs.RO_PO_PO_ST_ST_ACTIONS,
            IIDs.RO_PO_PO_ST_ST_AC_CONFIG,

            BgpIIDs.RO_PO_PO_ST_ST_AC_AUG_ACTIONS2,
            BgpIIDs.RO_PO_PO_ST_ST_AC_AUG_ACTIONS2_BGPACTIONS,
            BgpIIDs.RO_PO_PO_ST_ST_AC_AUG_ACTIONS2_BG_CONFIG,

            // As path prepend
            BgpIIDs.RO_PO_PO_ST_ST_AC_AUG_ACTIONS2_BG_SETASPATHPREPEND,
            BgpIIDs.RO_PO_PO_ST_ST_AC_AUG_ACTIONS2_BG_SE_CONFIG,

            // Community set
            BgpIIDs.RO_PO_PO_ST_ST_AC_AUG_ACTIONS2_BG_SETCOMMUNITY,
            BgpIIDs.ROU_POL_POL_STA_STA_ACT_AUG_ACTIONS2_BGP_SET_CONFIG,
            BgpIIDs.RO_PO_PO_ST_ST_AC_AUG_ACTIONS2_BG_SE_REFERENCE,
            BgpIIDs.RO_PO_PO_ST_ST_AC_AUG_ACTIONS2_BG_SE_RE_CONFIG,
            BgpIIDs.RO_PO_PO_ST_ST_AC_AUG_ACTIONS2_BG_SE_INLINE,
            BgpIIDs.RO_PO_PO_ST_ST_AC_AUG_ACTIONS2_BG_SE_IN_CONFIG)

        // Statements
        private val STATEMENTS_SUBTREES: Set<InstanceIdentifier<*>> = setOf(
            IIDs.RO_PO_PO_ST_ST_CONFIG,
            IIDs.RO_PO_PO_ST_ST_ACTIONS,
            IIDs.RO_PO_PO_ST_ST_AC_CONFIG,
            IIDs.RO_PO_PO_ST_STATEMENT)
            .plus(STATEMENTS_CONDITIONS_SUBTREES)
            .plus(STATEMENTS_ACTIONS_SUBTREES)
    }
}