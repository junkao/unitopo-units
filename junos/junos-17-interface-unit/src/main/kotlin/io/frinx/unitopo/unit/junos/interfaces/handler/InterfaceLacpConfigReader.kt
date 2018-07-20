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

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.fd.honeycomb.translate.read.ReadFailedException
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MDSalReadFailedEx
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.ethernet.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.lacp.mode.Case1
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.lacp.mode.Case2
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.lag.member.rev171109.Config1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.lag.member.rev171109.Config1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.LacpActivityType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.LacpPeriodType
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.Lacp
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.gigether.options.Ieee8023ad

class InterfaceLacpConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config1, Config1Builder> {
    override fun getBuilder(iid: InstanceIdentifier<Config1>): Config1Builder {
        return Config1Builder()
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(
        iid: InstanceIdentifier<Config1>,
        builder: Config1Builder,
        context: ReadContext
    ) {
        try {
            val name = iid.firstKeyOf(Interface::class.java).name
            InterfaceReader.readEthernetCfg(underlayAccess, name, {
                parseBundleId(it)?.let {
                    InterfaceReader.readAggregationCfg(underlayAccess, it, {
                        builder.fromUnderlay(it)
                    })
                }
            })
        } catch (e: MDSalReadFailedEx) {
            throw ReadFailedException(iid, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, data: Config1) {
        (builder as ConfigBuilder).addAugmentation(Config1::class.java, data)
    }

    companion object {
        fun parseBundleId(ethIface: Ieee8023ad): String? {
            return ethIface.bundle?.interfaceDevice?.value!!
        }
    }
}

internal fun Config1Builder.fromUnderlay(underlay: AggregatedEtherOptions) {
    underlay.lacp?.let {
        if (it.mode is Case1 && (it.mode as Case1).isActive) {
            lacpMode = LacpActivityType.ACTIVE
        } else if (it.mode is Case2 && (it.mode as Case2).isPassive) {
            lacpMode = LacpActivityType.PASSIVE
        }
        it.periodic?.let {
            if (it == Lacp.Periodic.Fast) {
                interval = LacpPeriodType.FAST
            } else if (it == Lacp.Periodic.Slow) {
                interval = LacpPeriodType.SLOW
            }
        }
    }
}