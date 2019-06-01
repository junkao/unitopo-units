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
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.bgp.handler.BgpProtocolReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.Aggregate
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpPrefixBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.juniper.routing.instance.RoutingOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.Instance
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.InstanceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.juniper.routing.options.Aggregate as JunosAggregate
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class BgpAggregateReader(private val access: UnderlayAccess) :
    CompositeListReader.Child<Aggregate, AggregateKey, AggregateBuilder> {

    override fun getCheck(): Check {
        return ChecksMap.PathCheck.Protocol.BGP
    }

    override fun getBuilder(id: IID<Aggregate>): AggregateBuilder {
        // NOOP
        throw UnsupportedOperationException("Should not be invoked")
    }

    override fun getAllIds(
        id: IID<Aggregate>,
        readContext: ReadContext
    ): List<AggregateKey> {

        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
            return emptyList()
        }

        val junosAggregate = access.read(
            BgpProtocolReader.JUNOS_VRFS_ID
                .child(Instance::class.java, InstanceKey(vrfKey.name))
                .child(RoutingOptions::class.java)
                .child(JunosAggregate::class.java))
            .checkedGet()
            .orNull()

        return parseJunosAggregates(junosAggregate)
    }

    override fun readCurrentAttributes(
        IID: IID<Aggregate>,
        aggregateBuilder: AggregateBuilder,
        readContext: ReadContext
    ) {
        aggregateBuilder.prefix = IID.firstKeyOf(Aggregate::class.java).prefix
    }

    companion object {
        private fun parseJunosAggregates(junosAggregate: JunosAggregate?): List<AggregateKey> {
            return junosAggregate?.route.orEmpty()
                .map { AggregateKey(IpPrefixBuilder.getDefaultInstance(it.name.value)) }
                .toList()
        }
    }
}