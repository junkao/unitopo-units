/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.interfaces.handler.aggregate

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr7.interfaces.handler.Util
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev190512._interface.configurations._interface.configuration.Lacp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev190512._interface.configurations._interface.configuration.LacpBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev190512.InterfaceConfiguration3 as LacpInterfaceConfig
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.mdrv.lib.cfg.rev190405.InterfaceConfiguration2 as MdrvInterfaceConfig
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.mdrv.lib.cfg.rev190405.InterfaceConfiguration2Builder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.ext.rev180926.IfLagAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.Objects

class AggregateConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext) {
        writeAggregateConfig(id, NULL_CONF, dataAfter)
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, dataBefore: Config, context: WriteContext) {
        writeAggregateConfig(id, dataBefore, NULL_CONF)
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeAggregateConfig(id, dataBefore, dataAfter)
    }

    private fun writeAggregateConfig(
        id: InstanceIdentifier<Config>,
        dataBefore: Config?,
        dataAfter: Config?
    ) {
        isSupportedForInterface(id)

        val ifLagAugBfr = dataBefore?.getAugmentation(IfLagAug::class.java)
        val ifLagAugAft = dataAfter?.getAugmentation(IfLagAug::class.java)
        if (!Objects.equals(ifLagAugBfr?.systemIdMac, ifLagAugAft?.systemIdMac)) {
            val (underlayAggrLacpId, underlayAggrLacp) = getLacpData(id, dataAfter)
            underlayAccess.put(underlayAggrLacpId, underlayAggrLacp)
        }

        if (!Objects.equals(ifLagAugBfr?.macAddress, ifLagAugAft?.macAddress)) {
            val (underlayAggrMacAddrId, underlayAggrMacAddr) = getMacAddrData(id, dataAfter)
            underlayAccess.put(underlayAggrMacAddrId, underlayAggrMacAddr)
        }
    }

    private fun getLacpBuilder(existingData: Lacp?): LacpBuilder {
        if (existingData == null) {
            return LacpBuilder()
        } else {
            return LacpBuilder(existingData)
        }
    }

    private fun getInterfaceConfigrationBuilder(existingData: InterfaceConfiguration?): InterfaceConfigurationBuilder {
        if (existingData == null) {
            return InterfaceConfigurationBuilder()
        } else {
            return InterfaceConfigurationBuilder(existingData)
        }
    }

    private fun getMacAddrBuilder(existingData: InterfaceConfigurationBuilder?): InterfaceConfiguration2Builder {
        val mdrvInterfaceConfig = existingData?.getAugmentation(MdrvInterfaceConfig::class.java)
        if (mdrvInterfaceConfig == null) {
            return InterfaceConfiguration2Builder()
        } else {
            return InterfaceConfiguration2Builder(mdrvInterfaceConfig)
        }
    }

    private fun getUnderlayLacpId(id: InstanceIdentifier<Config>): InstanceIdentifier<Lacp> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)

        return InterfaceReader.IFC_CFGS.child(
                InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
                .augmentation(LacpInterfaceConfig::class.java)
                .child(Lacp::class.java)
    }

    private fun getLacpData(
        id: InstanceIdentifier<Config>,
        dataAfter: Config?
    ): Pair<InstanceIdentifier<Lacp>, Lacp> {
        val underlayLacpId = getUnderlayLacpId(id)
        val existingData = underlayAccess.read(underlayLacpId).checkedGet().orNull()
        val systemIdMac = dataAfter?.getAugmentation(IfLagAug::class.java)?.systemIdMac
        val lacpBuilder = getLacpBuilder(existingData)

        lacpBuilder.setSystemMac(systemIdMac)

        return Pair(underlayLacpId, lacpBuilder.build())
    }

    private fun getMacAddrData(
        id: InstanceIdentifier<Config>,
        data: Config?
    ): Pair<InstanceIdentifier<InterfaceConfiguration>, InterfaceConfiguration> {
        val underlayMacAddrId = getUnderlayMacAddrId(id)
        val existingData = underlayAccess.read(underlayMacAddrId).checkedGet().orNull()
        val interfaceConfigurationBuilder = getInterfaceConfigrationBuilder(existingData)
        val macAddress = data?.getAugmentation(IfLagAug::class.java)?.macAddress
        val builder = getMacAddrBuilder(interfaceConfigurationBuilder)

        if (macAddress != null) {
            builder.setMacAddr(macAddress)
            interfaceConfigurationBuilder.addAugmentation(MdrvInterfaceConfig::class.java, builder.build())
        } else {
            interfaceConfigurationBuilder.removeAugmentation(MdrvInterfaceConfig::class.java)
        }
        return Pair(underlayMacAddrId, interfaceConfigurationBuilder.build())
    }

    private fun getUnderlayMacAddrId(id: InstanceIdentifier<Config>): InstanceIdentifier<InterfaceConfiguration> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)

        return InterfaceReader.IFC_CFGS.child(
                InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
    }

    private fun isSupportedForInterface(deviceId: InstanceIdentifier<Config>) {
        val ifcType = Util.parseIfcType(deviceId.firstKeyOf(Interface::class.java).name)
        require(ifcType === Ieee8023adLag::class.java) {
            "Cannot configure aggregate config on non LAG interface ${deviceId.firstKeyOf(Interface::class.java).name}"
        }
    }

    companion object {
        private val NULL_CONF: Config? = null
    }
}