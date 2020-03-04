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
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev190405.RoutingPolicy as XrRoutingPolicy
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev190405.routing.policy.RoutePolicies as XrRoutePolicies
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.definitions.top.policy.definitions.PolicyDefinition
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.definitions.top.policy.definitions.PolicyDefinitionBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.definitions.top.policy.definitions.PolicyDefinitionKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class PolicyDefinitionReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<PolicyDefinition, PolicyDefinitionKey, PolicyDefinitionBuilder> {

    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<PolicyDefinition>,
        builder: PolicyDefinitionBuilder,
        readContext: ReadContext
    ) {
        builder.name = instanceIdentifier.firstKeyOf(PolicyDefinition::class.java).name
    }

    override fun getAllIds(
        instanceIdentifier: InstanceIdentifier<PolicyDefinition>,
        readContext: ReadContext
    ): List<PolicyDefinitionKey> {
        return underlayAccess.read(ROUTE_POLICIES, LogicalDatastoreType.CONFIGURATION).checkedGet().orNull()
            ?.routePolicy.orEmpty()
            .map { PolicyDefinitionKey(it.routePolicyName.value) }
    }

    companion object {
        val ROUTE_POLICIES = InstanceIdentifier.create(XrRoutingPolicy::class.java)
            .child(XrRoutePolicies::class.java)!!
    }
}