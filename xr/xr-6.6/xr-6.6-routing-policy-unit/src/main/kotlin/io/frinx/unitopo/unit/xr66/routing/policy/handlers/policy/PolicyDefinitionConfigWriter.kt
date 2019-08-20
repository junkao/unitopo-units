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
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev170907.routing.policy.route.policies.RoutePolicy
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev170907.routing.policy.route.policies.RoutePolicyBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev170907.routing.policy.route.policies.RoutePolicyKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.RplPolicy
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.definitions.top.policy.definitions.PolicyDefinition
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.definitions.top.policy.definitions.policy.definition.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class PolicyDefinitionConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun updateCurrentAttributes(
        iid: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeCurrentAttributes(iid, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributes(iid: IID<Config>, dataBefore: Config, wtc: WriteContext) {
        val (underlayId, underlayData) = getData(iid, dataBefore)

        underlayAccess.safeDelete(underlayId, underlayData)
    }

    override fun writeCurrentAttributes(iid: IID<Config>, dataAfter: Config, wtc: WriteContext) {
        val (underlayId, underlayData) = getData(iid, dataAfter)

        underlayAccess.safePut(underlayId, underlayData)
    }

    private fun getData(id: IID<Config>, dataAfter: Config): Pair<IID<RoutePolicy>, RoutePolicy> {
        val policyName = id.firstKeyOf(PolicyDefinition::class.java).name
        val underlayId = getUnderlayId(policyName)

        val builder = RoutePolicyBuilder()
        builder.routePolicyName = CiscoIosXrString(policyName)
        builder.rplRoutePolicy = RplPolicy("route-policy $policyName\nend-policy")
        return Pair(underlayId, builder.build())
    }

    companion object {
        private fun getUnderlayId(policyName: String) =
            PolicyDefinitionReader.ROUTE_POLICIES
                .child(RoutePolicy::class.java, RoutePolicyKey(CiscoIosXrString(policyName)))
    }
}