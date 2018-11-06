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

package io.frinx.unitopo.unit.junos.bfd.handler

import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.junos.interfaces.handler.lag.aggregate.InterfaceAggregationBfdConfigWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.ext.rev180211.IfBfdExtAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.rev171117.bfd.top.bfd.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.rev171117.bfd.top.bfd.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Ipaddr
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetection
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetectionBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as INTFC
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class ConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(iid: InstanceIdentifier<Config>, config: Config, context: WriteContext) {
        checkInterfaceType(iid)
        val (underlayBfdLivenessDetectionId, underlayBfdLivenessDetectionCfg) = getData(iid, config)
        underlayAccess.put(underlayBfdLivenessDetectionId, underlayBfdLivenessDetectionCfg)
    }

    override fun deleteCurrentAttributes(iid: InstanceIdentifier<Config>, config: Config, context: WriteContext) {
        checkInterfaceType(iid)
        val underlayBfdLivenessDetectionId = getUnderlayId(iid)
        underlayAccess.delete(underlayBfdLivenessDetectionId)
    }

    override fun updateCurrentAttributes(
        iid: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        context: WriteContext
    ) {
        checkInterfaceType(iid)
        val (underlayBfdLivenessDetectionId, underlayBfdLivenessDetection) = getData(iid, dataAfter)
        underlayAccess.merge(underlayBfdLivenessDetectionId, underlayBfdLivenessDetection)
    }

    companion object {
        private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<BfdLivenessDetection>, BfdLivenessDetection> {
            val remoteAddressAugmentation = dataAfter.getAugmentation(IfBfdExtAug::class.java)!!
            val underlayBfdLivenessDetectionId = getUnderlayId(id)
            val bfdLivenessDetectionBuilder = BfdLivenessDetectionBuilder()

            bfdLivenessDetectionBuilder.localAddress = Ipaddr(dataAfter.localAddress?.ipv4Address?.value)
            bfdLivenessDetectionBuilder.neighbor = Ipaddr(remoteAddressAugmentation.remoteAddress?.ipv4Address?.value)
            bfdLivenessDetectionBuilder.multiplier =
                BfdLivenessDetection.Multiplier(dataAfter.detectionMultiplier?.toString())
            bfdLivenessDetectionBuilder.minimumInterval =
                BfdLivenessDetection.MinimumInterval(dataAfter.desiredMinimumTxInterval)

            return Pair(underlayBfdLivenessDetectionId, bfdLivenessDetectionBuilder.build())
        }

        private fun checkInterfaceType(iid: InstanceIdentifier<Config>) {
            val underlayBfdLivenessDetectionId = getUnderlayId(iid)
            Preconditions.checkArgument(
                InterfaceAggregationBfdConfigWriter.isSupportedForInterface(underlayBfdLivenessDetectionId),
                "BFD configuration is not supported for interface %s", iid
            )
        }

        private fun getUnderlayId(id: InstanceIdentifier<Config>): InstanceIdentifier<BfdLivenessDetection> {
            val ifcName = id.firstKeyOf(Interface::class.java).id
            return InterfaceReader.IFCS.child(INTFC::class.java, InterfaceKey(ifcName))
                .child(AggregatedEtherOptions::class.java)
                .child(BfdLivenessDetection::class.java)
        }
    }
}