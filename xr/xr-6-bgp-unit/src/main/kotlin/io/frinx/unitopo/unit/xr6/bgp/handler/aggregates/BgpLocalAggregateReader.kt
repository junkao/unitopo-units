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

package io.frinx.unitopo.unit.xr6.bgp.handler.aggregates

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.handlers.bgp.BgpListReader
import io.frinx.unitopo.unit.xr6.bgp.IID
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalAfiSafiReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.sourced.network.table.sourced.networks.SourcedNetwork
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.LocalAggregatesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.Aggregate
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpPrefix
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject

class BgpLocalAggregateReader(private val access: UnderlayAccess) :
    BgpListReader.BgpConfigListReader<Aggregate, AggregateKey, AggregateBuilder> {

    override fun getBuilder(id: IID<Aggregate>) = AggregateBuilder()

    override fun getAllIdsForType(
        id: IID<Aggregate>,
        readContext: ReadContext
    ): List<AggregateKey> {
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        val data = access.read(BgpProtocolReader.UNDERLAY_BGP
                .child(Instance::class.java, InstanceKey(CiscoIosXrString(protKey.name))))
                .checkedGet()
                .orNull()

        return parseAggregates(data, vrfKey)
    }

    override fun merge(builder: Builder<out DataObject>, list: List<Aggregate>) {
        (builder as LocalAggregatesBuilder).aggregate = list
    }

    override fun readCurrentAttributesForType(
        IID: IID<Aggregate>,
        aggregateBuilder: AggregateBuilder,
        readContext: ReadContext
    ) {
        aggregateBuilder.key = IID.firstKeyOf(Aggregate::class.java)
    }

    companion object {
        fun parseAggregates(data: Instance?, vrfKey: NetworkInstanceKey): List<AggregateKey> {
            val fourByteAs = BgpProtocolReader.getFirst4ByteAs(data)

            val networks = if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
                GlobalAfiSafiReader.getGlobalAfs(fourByteAs)
                        .flatMap { it.sourcedNetworks?.sourcedNetwork.orEmpty() }
            } else {
                GlobalAfiSafiReader.getVrfAfs(fourByteAs, vrfKey)
                        .flatMap { it.sourcedNetworks?.sourcedNetwork.orEmpty() }
            }

            return networks
                    .map { it.toPrefix() }
                    .map { AggregateKey(it) }
                    .toList()
        }
    }
}

fun SourcedNetwork.toPrefix(): IpPrefix {
    val ip = networkAddr?.ipv4Address?.value ?: networkAddr?.ipv6Address?.value
    return IpPrefix("$ip/$networkPrefix".toCharArray())
}