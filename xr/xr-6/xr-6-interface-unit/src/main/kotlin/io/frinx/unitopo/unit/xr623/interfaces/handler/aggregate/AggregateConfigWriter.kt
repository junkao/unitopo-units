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

package io.frinx.unitopo.unit.xr623.interfaces.handler.aggregate

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.Bundle
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.BundleBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216.InterfaceConfiguration2 as MinLinksInterfaceConfig
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.bundle.MinimumActiveBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.Objects

class AggregateConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext) {
        writeAggregateConfig(id, dataAfter)
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, dataBefore: Config, context: WriteContext) {
        val dataAfter = ConfigBuilder(dataBefore).apply {
            minLinks = null
        }.build()
        writeAggregateConfig(id, dataAfter)
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        if (!Objects.equals(dataBefore?.minLinks, dataAfter?.minLinks)) {
            writeAggregateConfig(id, dataAfter)
        }
    }

    private fun writeAggregateConfig(
        id: InstanceIdentifier<Config>,
        dataAfter: Config
    ) {
        val underlayIfcCfgId = getInterfaceId(id)
        val (underlayId, data) = getMinlinksData(underlayIfcCfgId, dataAfter)
        underlayAccess.put(underlayId, data)
    }

    private fun getInterfaceId(id: InstanceIdentifier<Config>): InstanceIdentifier<InterfaceConfiguration> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)
        return InterfaceReader.IFC_CFGS.child(
                InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
    }

    private fun getMinlinksData(
        underlayIfcCfgId: InstanceIdentifier<InterfaceConfiguration>,
        data: Config
    ): Pair<InstanceIdentifier<Bundle>, Bundle> {
        val bundleId = underlayIfcCfgId.augmentation(MinLinksInterfaceConfig::class.java)
                .child(Bundle::class.java)
        val existingData = underlayAccess.read(bundleId).checkedGet().orNull()

        var builder = when (existingData) {
            null -> BundleBuilder()
            else -> BundleBuilder(existingData)
        }

        var minimumActiveBuilder = when (existingData?.minimumActive) {
            null -> MinimumActiveBuilder()
            else -> MinimumActiveBuilder(existingData.minimumActive)
        }

        builder.apply {
            minimumActive = minimumActiveBuilder.apply {
                links = when (data.minLinks) {
                    null -> null
                    else -> data.minLinks.toLong()
                }
            }.build()
        }
        return Pair(bundleId, builder.build())
    }
}