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

package io.frinx.unitopo.unit.junos.interfaces.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.ethernet.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Config1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.lag.member.rev171109.LacpEthConfigAug
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.lacp.mode.Case1Builder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.lacp.mode.Case2Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.LacpActivityType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.LacpPeriodType
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.Lacp
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.LacpBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceLacpConfigWriter(private val underlayAccess: UnderlayAccess) :
    WriterCustomizer<LacpEthConfigAug> {

    override fun writeCurrentAttributes(
        id: InstanceIdentifier<LacpEthConfigAug>,
        dataAfter: LacpEthConfigAug,
        writeContext: WriteContext
    ) {
        val (underlayAggrEthOptId, underlayAggrEthOpt) = getData(id, dataAfter, writeContext)
        try {
            underlayAccess.put(underlayAggrEthOptId, underlayAggrEthOpt)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<LacpEthConfigAug>,
        config: LacpEthConfigAug,
        context: WriteContext
    ) {
        val bundleId = context.readBefore(RWUtils.cutId(id, Config::class.java)).get()
            ?.getAugmentation(Config1::class.java)?.aggregateId
        try {
            underlayAccess.delete(InterfaceReader.IFCS.child(Interface::class.java, InterfaceKey(bundleId))
                    .child(AggregatedEtherOptions::class.java).child(Lacp::class.java))
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<LacpEthConfigAug>,
        dataBefore: LacpEthConfigAug,
        dataAfter: LacpEthConfigAug,
        writeContext: WriteContext
    ) {
        val (underlayAggrEthOptId, underlayAggrEthOpt) = getData(id, dataAfter, writeContext)
        try {
            underlayAccess.merge(underlayAggrEthOptId, underlayAggrEthOpt)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(
        id: InstanceIdentifier<LacpEthConfigAug>,
        dataAfter: LacpEthConfigAug,
        context: WriteContext
    ):
            Pair<InstanceIdentifier<Lacp>, Lacp> {
        val underlayAggrEthOptId = getUnderlayId(id, context)

        val lacpBuilder = LacpBuilder()
        dataAfter.lacpMode?.let {
            if (it == LacpActivityType.ACTIVE) {
                lacpBuilder.mode = Case1Builder().setActive(true).build()
            } else if (it == LacpActivityType.PASSIVE) {
                lacpBuilder.mode = Case2Builder().setPassive(true).build()
            }
        }
        dataAfter.interval?.let {
            if (it == LacpPeriodType.FAST) {
                lacpBuilder.periodic = Lacp.Periodic.Fast
            } else if (it == LacpPeriodType.SLOW) {
                lacpBuilder.periodic = Lacp.Periodic.Slow
            }
        }
        return Pair(underlayAggrEthOptId, lacpBuilder.build())
    }

    private fun getUnderlayId(id: InstanceIdentifier<LacpEthConfigAug>, context: WriteContext):
        InstanceIdentifier<Lacp> {
        val bundleId = context.readAfter(RWUtils.cutId(id, Config::class.java)).get()
            ?.getAugmentation(Config1::class.java)?.aggregateId
        return InterfaceReader.IFCS.child(Interface::class.java, InterfaceKey(bundleId))
            .child(AggregatedEtherOptions::class.java).child(Lacp::class.java)
    }
}