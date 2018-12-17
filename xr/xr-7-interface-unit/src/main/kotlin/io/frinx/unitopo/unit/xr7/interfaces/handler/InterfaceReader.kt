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

package io.frinx.unitopo.unit.xr7.interfaces.handler

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import java.util.regex.Pattern
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class InterfaceReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    override fun getAllIds(instanceIdentifier: IID<Interface>, readContext: ReadContext): List<InterfaceKey> {
        val configurations = underlayAccess.read(IFC_CFGS, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
        return getInterfaceIds(configurations).filter { !isSubinterface(it.name) }
    }

    override fun merge(builder: Builder<out DataObject>, list: List<Interface>) {
        (builder as InterfacesBuilder).`interface` = list
    }

    override fun getBuilder(instanceIdentifier: IID<Interface>): InterfaceBuilder = InterfaceBuilder()

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Interface>,
        interfaceBuilder: InterfaceBuilder,
        readContext: ReadContext
    ) {
        val configurations = underlayAccess.read(IFC_CFGS, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()

        // Just set the name (if there is such interface)
        if (interfaceExists(configurations, instanceIdentifier)) {
            interfaceBuilder.name = getInterfaceName(instanceIdentifier.firstKeyOf(Interface::class.java).name)
        }
    }

    companion object {
        val IFC_CFGS = IID.create(InterfaceConfigurations::class.java)!!
        val SUBINTERFACE_NAME = Pattern.compile("(?<ifcId>.+)[.](?<subifcIndex>[0-9]+)")

        /**
         * Read interface configuration
         */
        fun readInterfaceCfg(
            underlayAccess: UnderlayAccess,
            name: String,
            handler: (InterfaceConfiguration) -> kotlin.Unit
        ) {
            val configurations = underlayAccess.read(IFC_CFGS, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()

            // Check if reading existing interface because later we use default value for existing interfaces that
            // have no configuration
            if (!interfaceExists(configurations, name)) {
                return
            }

            configurations?.let {
                it.`interfaceConfiguration`.orEmpty()
                    .firstOrNull { it.interfaceName.value == name }
                    .let { handler(it ?: getDefaultIfcCfg(name)) }
            }
        }

        fun interfaceExists(configurations: InterfaceConfigurations?, name: String) =
            getInterfaceIds(configurations).contains(InterfaceKey(name))

        fun interfaceExists(configurations: InterfaceConfigurations?, name: IID<out DataObject>) =
            getInterfaceIds(configurations).contains(name.firstKeyOf(Interface::class.java)!!)

        private fun getDefaultIfcCfg(name: String): InterfaceConfiguration {
            return InterfaceConfigurationBuilder().apply {
                interfaceName = InterfaceName(name)
                isShutdown = null
            }.build()
        }

        /**
         * Read all interface IDs.
         */
        fun getInterfaceIds(configurations: InterfaceConfigurations?): List<InterfaceKey> {
            return configurations
                ?.let { parseInterfaceIds(it) }.orEmpty()
        }

        @VisibleForTesting
        fun parseInterfaceIds(it: InterfaceConfigurations): List<InterfaceKey> {
            return it.interfaceConfiguration
                .orEmpty()
                .map {
                    InterfaceKey(it.interfaceName.value)
                }.toList()
        }

        fun isSubinterface(name: String): Boolean {
            return SUBINTERFACE_NAME.matcher(name).matches()
        }

        fun getInterfaceName(name: String): String {
            val matcher = SUBINTERFACE_NAME.matcher(name)
            return when (matcher.matches()) {
                true -> matcher.group("ifcId")
                else -> name
            }
        }

        fun getSubinterfaceKey(name: String): SubinterfaceKey {
            val matcher = SUBINTERFACE_NAME.matcher(name)

            Preconditions.checkState(matcher.matches())
            return SubinterfaceKey(matcher.group("subifcIndex").toLong())
        }
    }
}

internal fun parseIfcType(name: String): Class<out InterfaceType> {
    return when {
        name.startsWith("HundredGigE") -> EthernetCsmacd::class.java
        name.startsWith("TenGigE") -> EthernetCsmacd::class.java
        name.startsWith("GigabitEthernet") -> EthernetCsmacd::class.java
        name.startsWith("Bundle-Ether") -> Ieee8023adLag::class.java
        else -> Other::class.java
    }
}