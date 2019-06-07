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

package io.frinx.unitopo.unit.xr66.interfaces.handler.ethernet

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr66.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501.BundlePortActivity
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501.PeriodShortEnum
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501._interface.configurations._interface.configuration.BundleMember
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501._interface.configurations._interface.configuration.Lacp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501._interface.configurations._interface.configuration.LacpBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501._interface.configurations._interface.configuration.bundle.member.Id
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501._interface.configurations._interface.configuration.bundle.member.IdBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Config1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.ethernet.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.LacpActivityType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.LacpPeriodType
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.regex.Pattern
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501.InterfaceConfiguration3 as LacpInterfaceConfig
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501.InterfaceConfiguration4 as BundleManagerInterfaceConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.lag.member.rev171109.LacpEthConfigAug as lacpConfig1

open class EthernetConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext) {
        val aggregationAug = dataAfter.getAugmentation(Config1::class.java)
        val lacpAug = dataAfter.getAugmentation(lacpConfig1::class.java)
        val ifcName = id.firstKeyOf(Interface::class.java).name

        if (aggregationAug != null) {
            val (bundleId, bundleData) = configureLAG(id, dataAfter)
            underlayAccess.merge(bundleId, bundleData)
        }

        if (lacpAug != null && lacpAug.interval != null) {
            val (lacpId, lacpData) = configureLacp(ifcName, id, dataAfter)
            underlayAccess.merge(lacpId, lacpData)
        }
    }

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val aggregationAugBefore = dataBefore.getAugmentation(Config1::class.java)
        val lacpAugBefore = dataBefore.getAugmentation(lacpConfig1::class.java)
        if (aggregationAugBefore != null) {
            underlayAccess.delete(deleteBundleMember(ifcName))
        }
        if (lacpAugBefore.interval != null) {
            underlayAccess.delete(deleteLacp(ifcName))
        }
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val aggregationAugBefore = dataBefore.getAugmentation(Config1::class.java)
        val lacpAugBefore = dataBefore.getAugmentation(lacpConfig1::class.java)
        val aggregationAug = dataAfter.getAugmentation(Config1::class.java)
        val lacpAug = dataAfter.getAugmentation(lacpConfig1::class.java)
        val ifcName = id.firstKeyOf(Interface::class.java).name

        if (aggregationAug == null && lacpAug == null) {
            deleteCurrentAttributes(id, dataBefore, writeContext)
        } else {
            if (aggregationAug != null) {
                val (bundleId, bundleData) = configureLAG(id, dataAfter)
                underlayAccess.merge(bundleId, bundleData)
            } else if (aggregationAugBefore != null) {
                underlayAccess.delete(deleteBundleMember(ifcName))
            }

            if (lacpAug != null && lacpAug.interval != null) {
                val (lacpId, lacpData) = configureLacp(ifcName, id, dataAfter)
                underlayAccess.merge(lacpId, lacpData)
            } else if ((lacpAugBefore != null && lacpAugBefore.interval != null) &&
                    (lacpAug == null || lacpAug.interval == null)) {
                underlayAccess.delete(deleteLacp(ifcName))
            }
        }
    }

    @Throws(WriteFailedException.CreateFailedException::class)
    private fun configureLAG(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<Id>, Id> {
        val aggregationAug = dataAfter.getAugmentation(Config1::class.java)
        val lacpAug = dataAfter.getAugmentation(lacpConfig1::class.java)
        val bundleId = getBundleId(aggregationAug)
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)
        val underlayIid = InterfaceReader.IFC_CFGS.child(InterfaceConfiguration::class.java,
                InterfaceConfigurationKey(interfaceActive, ifcName))
                .augmentation(BundleManagerInterfaceConfig::class.java).child(BundleMember::class.java)
                .child(Id::class.java)
        val idBuilder = IdBuilder().setBundleId(bundleId)
        idBuilder.fromOpenConfig(lacpAug)
        return Pair(underlayIid, idBuilder.build())
    }

    private fun configureLacp(ifcName: String, id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<Lacp>, Lacp> {
        val lacpAug = dataAfter.getAugmentation(lacpConfig1::class.java)
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)
        val underlayIid = InterfaceReader.IFC_CFGS.child(InterfaceConfiguration::class.java,
                InterfaceConfigurationKey(interfaceActive, ifcName))
                .augmentation(LacpInterfaceConfig::class.java).child(Lacp::class.java)
        val lacpBuilder = LacpBuilder()
        lacpBuilder.fromOpenConfig(lacpAug)
        return Pair(underlayIid, lacpBuilder.build())
    }

    private val AGGREGATE_IFC_NAME = Pattern.compile("Bundle-Ether(?<id>\\d+)")

    private fun getBundleId(aggregationAug: Config1?): Long? {
        if (aggregationAug == null || aggregationAug.aggregateId == null) {
            return null
        }

        val aggregateIfcName = aggregationAug.aggregateId
        val aggregateIfcNameMatcher = AGGREGATE_IFC_NAME.matcher(aggregateIfcName.trim())
        require(aggregateIfcNameMatcher.matches()) { "aggregate-id $aggregateIfcName should reference LAG interface" }

        return aggregateIfcNameMatcher.group("id").toLong()
    }
}

fun IdBuilder.fromOpenConfig(lacpConfig1: lacpConfig1?) {
    lacpConfig1 ?: return

    when (lacpConfig1.lacpMode) {
        LacpActivityType.ACTIVE -> setPortActivity(BundlePortActivity.Active)
        LacpActivityType.PASSIVE -> setPortActivity(BundlePortActivity.Passive)
    }
}

fun LacpBuilder.fromOpenConfig(lacpConfig1: lacpConfig1?) {
    lacpConfig1 ?: return
    if (lacpConfig1.interval == LacpPeriodType.FAST) {
        setPeriodShort(PeriodShortEnum(PeriodShortEnum.Enumeration.True))
    }
}

fun deleteBundleMember(ifcName: String): InstanceIdentifier<BundleMember> {
    val interfaceActive = InterfaceActive("act")

    return InterfaceReader.IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive,
                    InterfaceName(ifcName)))
            .augmentation(BundleManagerInterfaceConfig::class.java)
            .child(BundleMember::class.java)
}

fun deleteLacp(ifcName: String): InstanceIdentifier<Lacp> {
    val interfaceActive = InterfaceActive("act")

    return InterfaceReader.IFC_CFGS.child(InterfaceConfiguration::class.java,
            InterfaceConfigurationKey(interfaceActive, InterfaceName(ifcName)))
            .augmentation(LacpInterfaceConfig::class.java).child(Lacp::class.java)
}