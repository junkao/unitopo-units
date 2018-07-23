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

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.AggregationBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.rev171024.IfLagJuniperAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.rev171024.IfLagJuniperAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.rev171024.JuniperIfAggregateConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MDSalReadFailed
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptions as JunosAggregatedEtherOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptions.LinkSpeed as JunosAggregatedEtherOptionsLinkSpeed
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class InterfaceAggregationConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {
    override fun getBuilder(iid: IID<Config>): ConfigBuilder {
        return ConfigBuilder()
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(iid: IID<Config>, builder: ConfigBuilder, context: ReadContext) {
        try {
            val name = iid.firstKeyOf(Interface::class.java).name
            InterfaceReader.readAggregationCfg(underlayAccess, name, { builder.fromUnderlay(it) })
        } catch (e: MDSalReadFailed) {
            throw ReadFailedException(iid, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, data: Config) {
        (builder as AggregationBuilder).config = data
    }
}

private fun ConfigBuilder.fromUnderlay(underlay: JunosAggregatedEtherOptions?) {
    minLinks = underlay?.minimumLinks?.uint16
    addAugmentation(IfLagJuniperAug::class.java,
        IfLagJuniperAugBuilder().setLinkSpeed(parseLinkSpeed(underlay?.linkSpeed)).build())
}

internal fun parseLinkSpeed(linkSpeed: JunosAggregatedEtherOptionsLinkSpeed?): JuniperIfAggregateConfig.LinkSpeed? {
    return when (linkSpeed) {
        JunosAggregatedEtherOptionsLinkSpeed._10m -> JuniperIfAggregateConfig.LinkSpeed._10M
        JunosAggregatedEtherOptionsLinkSpeed._100m -> JuniperIfAggregateConfig.LinkSpeed._100M
        JunosAggregatedEtherOptionsLinkSpeed._1g -> JuniperIfAggregateConfig.LinkSpeed._1G
        JunosAggregatedEtherOptionsLinkSpeed._2g -> JuniperIfAggregateConfig.LinkSpeed._2G
        JunosAggregatedEtherOptionsLinkSpeed._5g -> JuniperIfAggregateConfig.LinkSpeed._5G
        JunosAggregatedEtherOptionsLinkSpeed._8g -> JuniperIfAggregateConfig.LinkSpeed._8G
        JunosAggregatedEtherOptionsLinkSpeed._10g -> JuniperIfAggregateConfig.LinkSpeed._10G
        JunosAggregatedEtherOptionsLinkSpeed._25g -> JuniperIfAggregateConfig.LinkSpeed._25G
        JunosAggregatedEtherOptionsLinkSpeed._40g -> JuniperIfAggregateConfig.LinkSpeed._40G
        JunosAggregatedEtherOptionsLinkSpeed._50g -> JuniperIfAggregateConfig.LinkSpeed._50G
        JunosAggregatedEtherOptionsLinkSpeed._80g -> JuniperIfAggregateConfig.LinkSpeed._80G
        JunosAggregatedEtherOptionsLinkSpeed._100g -> JuniperIfAggregateConfig.LinkSpeed._100G
        JunosAggregatedEtherOptionsLinkSpeed.Oc192 -> JuniperIfAggregateConfig.LinkSpeed.OC192
        JunosAggregatedEtherOptionsLinkSpeed.Mixed -> JuniperIfAggregateConfig.LinkSpeed.MIXED
        null -> null
    }
}