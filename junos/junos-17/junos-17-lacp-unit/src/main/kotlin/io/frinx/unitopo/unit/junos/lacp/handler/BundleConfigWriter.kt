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

package io.frinx.unitopo.unit.junos.lacp.handler

import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.lacp.mode.Case1Builder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.lacp.mode.Case2Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.LacpActivityType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.LacpPeriodType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.lacp.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.lacp.interfaces.top.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.Lacp
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.LacpBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class BundleConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(iid: InstanceIdentifier<Config>, config: Config, context: WriteContext) {
        isSupportedBundleId(iid)
        val (underlayBfdLivenessDetectionId, underlayBfdLivenessDetectionCfg) = getData(iid, config)
        underlayAccess.put(underlayBfdLivenessDetectionId, underlayBfdLivenessDetectionCfg)
    }

    override fun deleteCurrentAttributes(iid: InstanceIdentifier<Config>, config: Config, context: WriteContext) {
        isSupportedBundleId(iid)
        val underlayBfdLivenessDetectionId = getUnderlayId(iid)
        underlayAccess.delete(underlayBfdLivenessDetectionId)
    }

    override fun updateCurrentAttributes(
        iid: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        context: WriteContext
    ) {
        isSupportedBundleId(iid)
        val (underlayBfdLivenessDetectionId, underlayBfdLivenessDetectionCfg) = getData(iid, dataAfter)
        underlayAccess.merge(underlayBfdLivenessDetectionId, underlayBfdLivenessDetectionCfg)
    }

    companion object {
        private fun getData(iid: InstanceIdentifier<Config>, dataAfter: Config): Pair<InstanceIdentifier<Lacp>, Lacp> {
            val underlayId = getUnderlayId(iid)
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
            return Pair(underlayId, lacpBuilder.build())
        }

        private fun getUnderlayId(id: InstanceIdentifier<Config>): InstanceIdentifier<Lacp> {
            val bundleId = id.firstKeyOf(Interface::class.java).name
            return InterfaceReader.IFCS.child(JunosInterface::class.java, InterfaceKey(bundleId))
                .child(AggregatedEtherOptions::class.java)
                .child(Lacp::class.java)
        }

        private fun isSupportedBundleId(iid: InstanceIdentifier<Config>) {
            val bundleId = iid.firstKeyOf(Interface::class.java).name
            val ifcType = InterfaceConfigReader.parseIfcType(bundleId)
            val isSupportedBundleId = when (InterfaceConfigReader.parseIfcType(bundleId)) {
                Ieee8023adLag::class.java -> true
                else -> false
            }
            Preconditions.checkArgument(
                isSupportedBundleId,
                """LACP configuration is supported only on bundle interfaces.
                Cannot configure interface $bundleId of type $ifcType""".trimIndent()
            )
        }
    }
}