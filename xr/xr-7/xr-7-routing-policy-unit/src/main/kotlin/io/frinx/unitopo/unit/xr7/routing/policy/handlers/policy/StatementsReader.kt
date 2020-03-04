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

package io.frinx.unitopo.unit.xr7.routing.policy.handlers.policy

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.translate.unit.iosxr.route.policy.util.StatementsParser
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev190405.routing.policy.route.policies.RoutePolicy
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev190405.routing.policy.route.policies.RoutePolicyKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.definitions.top.policy.definitions.PolicyDefinition
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.statements.top.Statements
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.statements.top.StatementsBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class StatementsReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Statements, StatementsBuilder> {

    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Statements>,
        builder: StatementsBuilder,
        readContext: ReadContext
    ) {
        val policyName = instanceIdentifier.firstKeyOf(PolicyDefinition::class.java)!!.name
        val underlayId = getUnderlayId(policyName)

        val policy = underlayAccess.read(underlayId, LogicalDatastoreType.CONFIGURATION).checkedGet().orNull()
            ?.rplRoutePolicy?.value

        StatementsParser.parseOutput(policy, builder)
    }

    companion object {
        fun getUnderlayId(policyName: String) = PolicyDefinitionReader.ROUTE_POLICIES
            .child(RoutePolicy::class.java, RoutePolicyKey(CiscoIosXrString(policyName)))
    }
}