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

package io.frinx.unitopo.unit.xr6.interfaces.handler.aggregate

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.parseIfcType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.BundleBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216.InterfaceConfiguration2 as MinLinksInterfaceConfig
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216.InterfaceConfiguration2Builder as MinLinksInterfaceConfigBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.bundle.MinimumActiveBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
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

        if (!Objects.equals(dataBefore?.minLinks, dataAfter?.minLinks)) {
            val (minlinksId, minlinks) = getMinlinksData(id, dataAfter)
            underlayAccess.put(minlinksId, minlinks)
        }
    }

    private fun getInterfaceConfigrationBuilder(
        interfaceName: String?,
        existingData: InterfaceConfiguration?
    ): InterfaceConfigurationBuilder {
        require(existingData != null, { "Interface $interfaceName must be existing." })
        return InterfaceConfigurationBuilder(existingData)
    }

    private fun getInterfaceId(id: InstanceIdentifier<Config>): InstanceIdentifier<InterfaceConfiguration> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)

        return InterfaceReader.IFC_CFGS.child(
                InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
    }

    private fun isSupportedForInterface(deviceId: InstanceIdentifier<Config>) {
        val ifcType = parseIfcType(deviceId.firstKeyOf(Interface::class.java).name)
        require(ifcType === Ieee8023adLag::class.java) {
            "Cannot configure aggregate config on non LAG interface ${deviceId.firstKeyOf(Interface::class.java).name}"
        }
    }

    private fun getMinlinksData(
        id: InstanceIdentifier<Config>,
        data: Config?
    ): Pair<InstanceIdentifier<InterfaceConfiguration>, InterfaceConfiguration> {
        val minlinksId = getInterfaceId(id)
        val existingData = underlayAccess.read(minlinksId).checkedGet().orNull()
        val interfaceConfigurationBuilder = getInterfaceConfigrationBuilder(
                id.firstKeyOf(Interface::class.java).name, existingData)
        val minLinks = data?.minLinks
        val builder = getMinLinkBuilder(interfaceConfigurationBuilder)

        if (minLinks != null) {
            builder.setBundle(BundleBuilder()
                    .setMinimumActive(MinimumActiveBuilder()
                            .setLinks(minLinks.toLong())
                            .build())
                    .build())
            interfaceConfigurationBuilder.addAugmentation(MinLinksInterfaceConfig::class.java, builder.build())
        } else {
            interfaceConfigurationBuilder.removeAugmentation(MinLinksInterfaceConfig::class.java)
        }
        return Pair(minlinksId, interfaceConfigurationBuilder.setInterfaceModeNonPhysical(null).build())
    }

    private fun getMinLinkBuilder(existingData: InterfaceConfigurationBuilder?): MinLinksInterfaceConfigBuilder {
        val minLinksInterfaceConfig = existingData?.getAugmentation(MinLinksInterfaceConfig::class.java)
        if (minLinksInterfaceConfig == null) {
            return MinLinksInterfaceConfigBuilder()
        } else {
            return MinLinksInterfaceConfigBuilder(minLinksInterfaceConfig)
        }
    }

    companion object {
        private val NULL_CONF: Config? = null
    }
}