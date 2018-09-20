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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.junos.interfaces.handler.parseIfcType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.rev171024.IfLagJuniperAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.rev171024.JuniperIfAggregateConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptions as JunosAggregEthOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptions.MinimumLinks as JunosMinimumLinks
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptionsBuilder as JunosAggregEthOptionsBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey as JunosInterfaceKey

class InterfaceAggregationConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext) {
        isSupportedForInterface(id)
        val (underlayAggrEthOptId, underlayAggrEthOpt) = getData(id, dataAfter)
        underlayAccess.put(underlayAggrEthOptId, underlayAggrEthOpt)
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, config: Config, context: WriteContext) {
        isSupportedForInterface(id)
        val underlayAggrEthOptId = getUnderlayId(id)
        underlayAccess.delete(underlayAggrEthOptId)
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        isSupportedForInterface(id)
        val (underlayAggrEthOptId, underlayAggrEthOpt) = getData(id, dataAfter)
        underlayAccess.merge(underlayAggrEthOptId, underlayAggrEthOpt)
    }

    private fun getData(
        id: InstanceIdentifier<Config>,
        dataAfter: Config
    ): Pair<InstanceIdentifier<JunosAggregEthOptions>, JunosAggregEthOptions> {
        val underlayAggrEthOptId = getUnderlayId(id)

        val aggregatedEtherOptionsBuilder = JunosAggregEthOptionsBuilder()
        aggregatedEtherOptionsBuilder.minimumLinks = JunosMinimumLinks(dataAfter.minLinks)
        aggregatedEtherOptionsBuilder.linkSpeed =
            parseLinkSpeedJunos(dataAfter.getAugmentation(IfLagJuniperAug::class.java)?.linkSpeed)

        return Pair(underlayAggrEthOptId, aggregatedEtherOptionsBuilder.build())
    }

    private fun parseLinkSpeedJunos(linkSpeed: JuniperIfAggregateConfig.LinkSpeed?): JunosAggregEthOptions.LinkSpeed? {
        return when (linkSpeed) {
            JuniperIfAggregateConfig.LinkSpeed._10M -> JunosAggregEthOptions.LinkSpeed._10m
            JuniperIfAggregateConfig.LinkSpeed._100M -> JunosAggregEthOptions.LinkSpeed._100m
            JuniperIfAggregateConfig.LinkSpeed._1G -> JunosAggregEthOptions.LinkSpeed._1g
            JuniperIfAggregateConfig.LinkSpeed._2G -> JunosAggregEthOptions.LinkSpeed._2g
            JuniperIfAggregateConfig.LinkSpeed._5G -> JunosAggregEthOptions.LinkSpeed._5g
            JuniperIfAggregateConfig.LinkSpeed._8G -> JunosAggregEthOptions.LinkSpeed._8g
            JuniperIfAggregateConfig.LinkSpeed._10G -> JunosAggregEthOptions.LinkSpeed._10g
            JuniperIfAggregateConfig.LinkSpeed._25G -> JunosAggregEthOptions.LinkSpeed._25g
            JuniperIfAggregateConfig.LinkSpeed._40G -> JunosAggregEthOptions.LinkSpeed._40g
            JuniperIfAggregateConfig.LinkSpeed._50G -> JunosAggregEthOptions.LinkSpeed._50g
            JuniperIfAggregateConfig.LinkSpeed._80G -> JunosAggregEthOptions.LinkSpeed._80g
            JuniperIfAggregateConfig.LinkSpeed._100G -> JunosAggregEthOptions.LinkSpeed._100g
            JuniperIfAggregateConfig.LinkSpeed.OC192 -> JunosAggregEthOptions.LinkSpeed.Oc192
            JuniperIfAggregateConfig.LinkSpeed.MIXED -> JunosAggregEthOptions.LinkSpeed.Mixed
            else -> null
        }
    }

    private fun getUnderlayId(id: InstanceIdentifier<Config>): InstanceIdentifier<JunosAggregEthOptions> {
        val ifcName = id.firstKeyOf(Interface::class.java).name

        return InterfaceReader.IFCS.child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
            .child(JunosAggregEthOptions::class.java)
    }

    private fun isSupportedForInterface(deviceId: InstanceIdentifier<Config>) {
        val ifcType = parseIfcType(deviceId.firstKeyOf(Interface::class.java).name)
        require(ifcType === Ieee8023adLag::class.java) {
            "Cannot configure aggregate config on non LAG interface ${deviceId.firstKeyOf(Interface::class.java).name}"
        }
    }
}