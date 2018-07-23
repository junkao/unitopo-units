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

package io.frinx.unitopo.unit.xr6.interfaces.handler

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730.InterfaceProperties
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730._interface.properties.DataNodes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import java.util.regex.Pattern
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730._interface.table.interfaces.Interface as UnderlayInterface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class InterfaceReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    @Throws(ReadFailedException::class)
    override fun getAllIds(instanceIdentifier: IID<Interface>, readContext: ReadContext): List<InterfaceKey> {
        try {
            return getInterfaceIds(underlayAccess).filter { !isSubinterface(it.name) }
        } catch (e: org.opendaylight.controller.md.sal.common.api.data.ReadFailedException) {
            throw ReadFailedException(instanceIdentifier, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, list: List<Interface>) {
        (builder as InterfacesBuilder).`interface` = list
    }

    override fun getBuilder(instanceIdentifier: IID<Interface>): InterfaceBuilder = InterfaceBuilder()

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(
        instanceIdentifier: IID<Interface>,
        interfaceBuilder: InterfaceBuilder,
        readContext: ReadContext
    ) {
        try {
            // Just set the name (if there is such interface)
            if (interfaceExists(underlayAccess, instanceIdentifier)) {
                interfaceBuilder.name = instanceIdentifier.firstKeyOf(Interface::class.java).name
            }
        } catch (e: org.opendaylight.controller.md.sal.common.api.data.ReadFailedException) {
            throw ReadFailedException(instanceIdentifier, e)
        }
    }

    companion object {
        val IFC_CFGS = IID.create(InterfaceConfigurations::class.java)!!
        val DATA_NODES_ID = IID.create(InterfaceProperties::class.java).child(DataNodes::class.java)!!
        private val SUBINTERFACE_NAME = Pattern.compile("(?<ifcId>.+)[.](?<subifcIndex>[0-9]+)")

        /**
         * Read interface configuration
         */
        fun readInterfaceCfg(
            underlayAccess: UnderlayAccess,
            name: String,
            handler: (InterfaceConfiguration) -> kotlin.Unit
        ) {

            // Check if reading existing interface because later we use default value for existing interfaces that
            // have no configuration
            if (!interfaceExists(underlayAccess, name)) {
                return
            }

            underlayAccess.read(IFC_CFGS, LogicalDatastoreType.CONFIGURATION)
                    .checkedGet()
                    .orNull()
                    ?.let {
                        it.`interfaceConfiguration`.orEmpty()
                                .firstOrNull { it.interfaceName.value == name }
                                // Invoke handler with read value or use default
                                // XR returns no config data for interface that has no configuration but is up
                                .let { handler(it ?: getDefaultIfcCfg(name)) }
                    }
        }

        fun interfaceExists(underlayAccess: UnderlayAccess, name: String) =
                getInterfaceIds(underlayAccess).contains(InterfaceKey(name))

        fun interfaceExists(underlayAccess: UnderlayAccess, name: IID<out DataObject>) =
                getInterfaceIds(underlayAccess).contains(name.firstKeyOf(Interface::class.java)!!)

        private fun getDefaultIfcCfg(name: String): InterfaceConfiguration {
            return InterfaceConfigurationBuilder().apply {
                interfaceName = InterfaceName(name)
                isShutdown = null
            }.build()
        }

        /**
         * Read interface properties
         */
        fun readInterfaceProps(
            underlayAccess: UnderlayAccess,
            name: String,
            handler: (UnderlayInterface) -> kotlin.Unit
        ) {
            underlayAccess.read(DATA_NODES_ID, LogicalDatastoreType.OPERATIONAL)
                    .checkedGet()
                    .orNull()
                    ?.let {
                        it.dataNode.orEmpty()
                                .flatMap { it.systemView?.interfaces?.`interface`.orEmpty() }
                                .firstOrNull { it.interfaceName.value == name }
                                ?.let { handler(it) }
                    }
        }

        /**
         * Read all interface IDs.
         * Uses DATA_NODES_ID/interface-properties instead of IFC_CFGS because IFC_CFGS does not return un-configured
         * interfaces.
         */
        fun getInterfaceIds(underlayAccess: UnderlayAccess): List<InterfaceKey> {
            return underlayAccess.read(DATA_NODES_ID, LogicalDatastoreType.OPERATIONAL)
                    .checkedGet()
                    .orNull()
                    ?.let { parseInterfaceIds(it) }.orEmpty()
        }

        @VisibleForTesting
        fun parseInterfaceIds(it: DataNodes): List<InterfaceKey> {
            return it.dataNode.orEmpty()
                    .flatMap { it.systemView?.interfaces?.`interface`.orEmpty() }
                    .map { it.key }
                    .map { InterfaceKey(it.interfaceName.value) }
                    .toList()
        }

        fun isSubinterface(name: String): Boolean {
            return SUBINTERFACE_NAME.matcher(name).matches()
        }

        fun getSubinterfaceKey(name: String): SubinterfaceKey {
            val matcher = SUBINTERFACE_NAME.matcher(name)

            Preconditions.checkState(matcher.matches())
            return SubinterfaceKey(matcher.group("subifcIndex").toLong())
        }
    }
}

internal fun parseIfcType(name: String): Class<out InterfaceType> {
    // FIXME duplicate with ios-interface-unit
    return when {
        name.startsWith("MgmtEth") -> EthernetCsmacd::class.java
        name.startsWith("FastEther") -> EthernetCsmacd::class.java
        name.startsWith("GigabitEthernet") -> EthernetCsmacd::class.java
        name.startsWith("Loopback") -> SoftwareLoopback::class.java
        else -> Other::class.java
    }
}