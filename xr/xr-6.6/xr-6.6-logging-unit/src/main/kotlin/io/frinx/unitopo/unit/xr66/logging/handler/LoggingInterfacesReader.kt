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

package io.frinx.unitopo.unit.xr66.logging.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.event.types.rev171024.LINKUPDOWN
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging._interface.config.EnabledLoggingForEvent
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging._interface.config.EnabledLoggingForEventBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.interfaces.structural.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.interfaces.structural.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.interfaces.structural.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.interfaces.structural.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.top.LoggingBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.Collections

open class LoggingInterfacesReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Interfaces, InterfacesBuilder> {

    override fun getBuilder(p0: InstanceIdentifier<Interfaces>): InterfacesBuilder {
        return InterfacesBuilder()
    }

    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Interfaces>,
        builder: InterfacesBuilder,
        readContext: ReadContext
    ) {
        val names = readAllInterfaceNamesFromNative(underlayAccess)
        val ifcList = names.filter {
            it.startsWith("Bundle-Ether")
        }.filter {
            val key = InterfaceConfigurationKey(InterfaceActive("act"), InterfaceName(it))
            val iid = InstanceIdentifier
                    .create(InterfaceConfigurations::class.java)
                    .child(InterfaceConfiguration::class.java,
                        key)
            underlayAccess.read(iid, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()
                .let {
                    it?.isLinkStatus ?: false
                }
        }.map {
            InterfaceBuilder().apply {
                this.interfaceId = InterfaceId(it)
                this.config = getIfcConfig(this.interfaceId)
            }.build()
        }.toList()

        if (ifcList.isNotEmpty()) {
            builder.setInterface(ifcList)
        }
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Interfaces) {
        (parentBuilder as LoggingBuilder).setInterfaces(readValue)
    }

    private fun getIfcConfig(ifcId: InterfaceId): Config {
        return ConfigBuilder()
            .setInterfaceId(ifcId)
            .setEnabledLoggingForEvent(LINK_UP_DOWN_EVENT_LIST)
            .build()
    }

    private fun readAllInterfaceNamesFromNative(
        underlayAccess: UnderlayAccess
    ): List<String> {
        val configurations = underlayAccess.read(IFC_CFGS, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
            .let {
                it?.interfaceConfiguration?.map {
                    it.interfaceName.value
                }
            }?.toList()
        return configurations.orEmpty()
    }

    companion object {
        val IFC_CFGS = InstanceIdentifier.create(InterfaceConfigurations::class.java)!!

        private val LINK_UP_DOWN_EVENT: EnabledLoggingForEvent = EnabledLoggingForEventBuilder()
            .setEventName(LINKUPDOWN::class.java)
            .build()

        @VisibleForTesting
        val LINK_UP_DOWN_EVENT_LIST: List<EnabledLoggingForEvent> = Collections.singletonList(LINK_UP_DOWN_EVENT)
    }
}