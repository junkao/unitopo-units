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

package io.frinx.unitopo.unit.xr66.routing.policy.handlers.policy

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.translate.unit.iosxr.route.policy.util.StatementsRenderer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev170907.routing.policy.route.policies.RoutePolicy
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev170907.routing.policy.route.policies.RoutePolicyBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev170907.routing.policy.route.policies.RoutePolicyKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.RplPolicy
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.definitions.top.policy.definitions.PolicyDefinition
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.statements.top.Statements
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class StatementsWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Statements> {
    override fun updateCurrentAttributes(
        iid: IID<Statements>,
        dataBefore: Statements,
        dataAfter: Statements,
        writeContext: WriteContext
    ) {
        val (underlayId, underlayDataBefore) = getData(iid, dataBefore)
        val (_, underlayDataAfter) = getData(iid, dataAfter)

        underlayAccess.safeMerge(underlayId, underlayDataBefore, underlayId, underlayDataAfter)
    }

    override fun deleteCurrentAttributes(iid: IID<Statements>, dataBefore: Statements, wtc: WriteContext) {
        val policyName = iid.firstKeyOf(PolicyDefinition::class.java).name

        val (underlayId, underlayDataBefore) = getData(iid, dataBefore)
        val underlayEmptyData = PolicyDefinitionConfigWriter.getUnderlayEmptyPolicy(policyName)

        underlayAccess.safeMerge(underlayId, underlayDataBefore, underlayId, underlayEmptyData)
    }

    override fun writeCurrentAttributes(iid: IID<Statements>, dataAfter: Statements, wtc: WriteContext) {
        val policyName = iid.firstKeyOf(PolicyDefinition::class.java).name

        val (underlayId, underlayData) = getData(iid, dataAfter)
        val underlayEmptyData = PolicyDefinitionConfigWriter.getUnderlayEmptyPolicy(policyName)

        underlayAccess.safeMerge(underlayId, underlayEmptyData, underlayId, underlayData)
    }

    private fun getData(id: IID<Statements>, dataAfter: Statements): Pair<IID<RoutePolicy>, RoutePolicy> {
        val policyName = id.firstKeyOf(PolicyDefinition::class.java).name
        val underlayId = getUnderlayId(policyName)

        val builder = RoutePolicyBuilder()
        builder.routePolicyName = CiscoIosXrString(policyName)
        builder.rplRoutePolicy = RplPolicy(StatementsRenderer.renderStatements(dataAfter.statement, policyName))
        return Pair(underlayId, builder.build())
    }

    companion object {
        private fun getUnderlayId(policyName: String) =
            PolicyDefinitionReader.ROUTE_POLICIES
                .child(RoutePolicy::class.java, RoutePolicyKey(CiscoIosXrString(policyName)))
    }
}