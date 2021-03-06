/*
 * Copyright © 2019 Frinx and others.
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

package io.frinx.unitopo.unit.xr623.interfaces.handler.ethernet

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.Util
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.ethernet.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.ethernet.ConfigBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216.BundlePortActivity
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216.PeriodShortEnum
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216.InterfaceConfiguration3 as LacpInterfaceConfig
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216.InterfaceConfiguration4 as BundleManagerInterfaceConfig
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.BundleMember
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Config1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Config1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.LacpActivityType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.LacpPeriodType
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.lag.member.rev171109.LacpEthConfigAug as lacpConfig1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.lag.member.rev171109.LacpEthConfigAugBuilder as lacpConfig1Builder

open class EthernetConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val ifcName = instanceIdentifier.firstKeyOf(Interface::class.java).name
        if (!PHYS_IFC_TYPES.contains(Util.parseIfcType(ifcName))) {
            return
        }
        InterfaceReader.readInterfaceCfg(underlayAccess, ifcName, { configBuilder.fromUnderlay(it) })
    }

    companion object {
        val IFC_CFGS = IID.create(BundleMember::class.java)!!
        val PHYS_IFC_TYPES = setOf(EthernetCsmacd::class.java)
    }
}

fun ConfigBuilder.fromUnderlay(underlay: InterfaceConfiguration) {
    if (underlay.getAugmentation(BundleManagerInterfaceConfig::class.java) != null) {
        val ethIfAggregationConfigBuilder = Config1Builder()
        ethIfAggregationConfigBuilder.aggregateId = "Bundle-Ether" +
                underlay.getAugmentation(BundleManagerInterfaceConfig::class.java).bundleMember.id.bundleId.toString()

        addAugmentation(Config1::class.java, ethIfAggregationConfigBuilder.build())
        val lacpEthConfigAugBuilder = lacpConfig1Builder()

        lacpEthConfigAugBuilder.setBundlePortActivity(underlay.getAugmentation(BundleManagerInterfaceConfig::class.java)
                .bundleMember.id.portActivity)

        if (underlay.getAugmentation(LacpInterfaceConfig::class.java) != null) {
            lacpEthConfigAugBuilder.setLacpPeriodShort(underlay.getAugmentation(LacpInterfaceConfig::class.java)
                    .lacp.periodShort)
        }

        if (!lacpEthConfigAugBuilder.isEmpty()) {
            addAugmentation(lacpConfig1::class.java, lacpEthConfigAugBuilder.build())
        }
    } else if (underlay.getAugmentation(LacpInterfaceConfig::class.java) != null) {

        val lacpEthConfigAugBuilder = lacpConfig1Builder()
        lacpEthConfigAugBuilder.setLacpPeriodShort(underlay.getAugmentation(LacpInterfaceConfig::class.java)
                .lacp.periodShort)

        if (!lacpEthConfigAugBuilder.isEmpty()) {
            addAugmentation(lacpConfig1::class.java, lacpEthConfigAugBuilder.build())
        }
    }
}

fun lacpConfig1Builder.setBundlePortActivity(activity: BundlePortActivity) {
    when (activity) {
        BundlePortActivity.Active -> setLacpMode(LacpActivityType.ACTIVE)
        BundlePortActivity.Passive -> setLacpMode(LacpActivityType.PASSIVE)
    }
}

fun lacpConfig1Builder.isEmpty(): Boolean {
    return when {
        lacpMode != null || interval != null -> false
        else -> true
    }
}

fun lacpConfig1Builder.setLacpPeriodShort(periodshort: PeriodShortEnum) {
    when (periodshort.enumeration.intValue) {
        1 -> setInterval(LacpPeriodType.FAST)
        else -> setInterval(LacpPeriodType.SLOW)
    }
}