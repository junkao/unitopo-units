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

package io.frinx.unitopo.unit.junos18.bgp.handler.aggregate

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.builder.Check
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.bgp.handler.BgpProtocolReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.NiProtAggAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.NiProtAggAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.Aggregate
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.common.types.rev180101.Ipprefix
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.juniper.routing.instance.RoutingOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.rib_aggregate_type.Route
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.rib_aggregate_type.RouteKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.Instance
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.InstanceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.juniper.routing.options.Aggregate as JunosAggregate

class BgpAggregateConfigReader(private val underlayAccess: UnderlayAccess) :
        CompositeReader.Child<Config, ConfigBuilder> {

    override fun getCheck(): Check {
        return ChecksMap.PathCheck.Protocol.BGP
    }

    override fun getBuilder(id: InstanceIdentifier<Config>): ConfigBuilder {
        // NOOP
        throw UnsupportedOperationException("Should not be invoked")
    }

    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val (vrfKey, aggregateKey) = extractKeys(instanceIdentifier)
        val underlayId = getUnderlayId(vrfKey.name, String(aggregateKey.prefix.value))
        val niProtAggAugBuilder = NiProtAggAugBuilder()

        val route = underlayAccess.read(underlayId)
            .checkedGet()
            .orNull()

        configBuilder.prefix = aggregateKey.prefix
        niProtAggAugBuilder.isSummaryOnly = true
        niProtAggAugBuilder.fromUnderlay(route)

        configBuilder.addAugmentation(NiProtAggAug::class.java, niProtAggAugBuilder.build())
    }

    companion object {
        internal fun getUnderlayId(vrfName: String, ipprefix: String):
                InstanceIdentifier<Route> {

            return BgpProtocolReader.JUNOS_VRFS_ID
                .child(Instance::class.java, InstanceKey(vrfName))
                .child(RoutingOptions::class.java)
                .child(JunosAggregate::class.java)
                .child(Route::class.java, RouteKey(Ipprefix.getDefaultInstance(ipprefix)))
        }

        internal fun extractKeys(instanceIdentifier: InstanceIdentifier<Config>):
                Pair<NetworkInstanceKey, AggregateKey> {

            val vrfKey = instanceIdentifier.firstKeyOf(NetworkInstance::class.java)
            val aggregateKey = instanceIdentifier.firstKeyOf(Aggregate::class.java)

            return Pair(vrfKey, aggregateKey)
        }

        fun NiProtAggAugBuilder.fromUnderlay(route: Route?) {
            route ?: return

            this.applyPolicy = route.policy.orEmpty()
                .map { it.value }
                .toList()
        }
    }
}