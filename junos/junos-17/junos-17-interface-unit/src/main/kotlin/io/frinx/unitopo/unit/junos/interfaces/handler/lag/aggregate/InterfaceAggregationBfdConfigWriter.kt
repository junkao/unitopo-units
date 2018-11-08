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
package io.frinx.unitopo.unit.junos.interfaces.handler.lag.aggregate

import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.bfd.top.bfd.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Ipaddr
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptions as JunosAggregEthOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetection as JunosBfdLivenessDetection
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetection.MinimumInterval as JunosMinInterval
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetection.Multiplier as JunosMultiplier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetectionBuilder as JunosBfdLivenessDetectionBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey as JunosInterfaceKey

class InterfaceAggregationBfdConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (underlayBfdLivenessDetectionId, underlayBfdLivenessDetectionCfg) = getData(id, dataAfter)
        Preconditions.checkArgument(isSupportedForInterface(underlayBfdLivenessDetectionId),
                "Write: Aggregation Bfd Config is not supported for: %s", id)
        try {
            underlayAccess.put(underlayBfdLivenessDetectionId, underlayBfdLivenessDetectionCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, config: Config, context: WriteContext) {
        val underlayBfdLivenessDetectionId = getUnderlayId(id)
        Preconditions.checkArgument(isSupportedForInterface(underlayBfdLivenessDetectionId),
                "Delete: Aggregation Bfd Config is not supported for: %s", id)
        try {
            underlayAccess.delete(underlayBfdLivenessDetectionId)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (underlayBfdLivenessDetectionId, underlayBfdLivenessDetection) = getData(id, dataAfter)
        Preconditions.checkArgument(isSupportedForInterface(underlayBfdLivenessDetectionId),
            "Update: Aggregation Bfd Config is not supported for: %s", id)
        try {
            underlayAccess.merge(underlayBfdLivenessDetectionId, underlayBfdLivenessDetection)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<JunosBfdLivenessDetection>, JunosBfdLivenessDetection> {
        val underlayBfdLivenessDetectionId = getUnderlayId(id)

        val bfdLivenessDetectionBuilder = JunosBfdLivenessDetectionBuilder()
        bfdLivenessDetectionBuilder.localAddress = Ipaddr(dataAfter.localAddress?.ipv4Address?.value)
        bfdLivenessDetectionBuilder.neighbor = Ipaddr(dataAfter.destinationAddress?.ipv4Address?.value)
        bfdLivenessDetectionBuilder.multiplier = JunosMultiplier(dataAfter.multiplier)
        bfdLivenessDetectionBuilder.minimumInterval = JunosMinInterval(dataAfter.minInterval)

        return Pair(underlayBfdLivenessDetectionId, bfdLivenessDetectionBuilder.build())
    }

    private fun getUnderlayId(id: InstanceIdentifier<Config>): InstanceIdentifier<JunosBfdLivenessDetection> {
        val ifcName = id.firstKeyOf(Interface::class.java).name

        return InterfaceReader.IFCS.child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
                .child(JunosAggregEthOptions::class.java)
                .child(JunosBfdLivenessDetection::class.java)
    }

    companion object {
        fun isSupportedForInterface(deviceId: InstanceIdentifier<JunosBfdLivenessDetection>): Boolean {
            return when (InterfaceConfigReader.parseIfcType(deviceId.firstKeyOf(JunosInterface::class.java).name)) {
                Ieee8023adLag::class.java -> true
                else -> false
            }
        }
    }
}