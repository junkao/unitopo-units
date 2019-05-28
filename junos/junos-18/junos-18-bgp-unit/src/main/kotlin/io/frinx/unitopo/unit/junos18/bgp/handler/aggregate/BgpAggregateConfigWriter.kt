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

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.NiProtAggAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.common.types.rev180101.PolicyAlgebra
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.rib_aggregate_type.RouteBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.common.types.rev180101.Ipprefix as JunodIpPrefix
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class BgpAggregateConfigWriter(private val access: UnderlayAccess) : CompositeWriter.Child<Config> {

    override fun writeCurrentAttributesWResult(
        instanceIdentifier: IID<Config>,
        config: Config,
        writeContext: WriteContext
    ): Boolean {
        if (!ChecksMap.PathCheck.Protocol.BGP.canProcess(instanceIdentifier, writeContext, false)) {
            return false
        }

        val (vrfKey, aggregateKey) = BgpAggregateConfigReader.extractKeys(instanceIdentifier)
        val underlayId = BgpAggregateConfigReader.getUnderlayId(
            vrfKey.name,
            String(aggregateKey.prefix.value))
        val routeBuilder = RouteBuilder()

        routeBuilder.fromOpenConfig(config)
        access.put(underlayId, routeBuilder.build())
        return true
    }

    override fun updateCurrentAttributesWResult(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ): Boolean {
        return writeCurrentAttributesWResult(id, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributesWResult(
        instanceIdentifier: IID<Config>,
        config: Config,
        writeContext: WriteContext
    ): Boolean {
        if (!ChecksMap.PathCheck.Protocol.BGP.canProcess(instanceIdentifier, writeContext, true)) {
            return false
        }

        val (vrfKey, aggregateKey) = BgpAggregateConfigReader.extractKeys(instanceIdentifier)
        val underlayId = BgpAggregateConfigReader.getUnderlayId(
            vrfKey.name,
            String(aggregateKey.prefix.value))

        access.delete(underlayId)
        return true
    }

    companion object {
        private fun RouteBuilder.fromOpenConfig(config: Config) {
            name = JunodIpPrefix(String(config.prefix.value))

            val aug = config.getAugmentation(NiProtAggAug::class.java)
            val policy = aug?.let {
                it.applyPolicy.orEmpty()
                    .map { PolicyAlgebra(it) }
                    .toList()
            }
            setPolicy(policy)
        }
    }
}