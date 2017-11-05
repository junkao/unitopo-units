/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.interfaces

import com.google.common.annotations.VisibleForTesting
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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730._interface.table.interfaces.Interface as UnderlayInterface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class InterfaceReader(private val underlayAccess: UnderlayAccess) : ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    @Throws(ReadFailedException::class)
    override fun getAllIds(instanceIdentifier: IID<Interface>, readContext: ReadContext): List<InterfaceKey> {
        try {
            return getInterfaceIds(underlayAccess)
        } catch (e: org.opendaylight.controller.md.sal.common.api.data.ReadFailedException) {
            throw ReadFailedException(instanceIdentifier, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, list: List<Interface>) {
        (builder as InterfacesBuilder).`interface` = list
    }

    override fun getBuilder(instanceIdentifier: IID<Interface>): InterfaceBuilder = InterfaceBuilder()

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(instanceIdentifier: IID<Interface>,
                                       interfaceBuilder: InterfaceBuilder,
                                       readContext: ReadContext) {
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

        /**
         * Read interface configuration
         */
        fun readInterfaceCfg(underlayAccess: UnderlayAccess, name: String, handler: (InterfaceConfiguration) -> kotlin.Unit) {

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
        fun readInterfaceProps(underlayAccess: UnderlayAccess, name: String, handler: (UnderlayInterface) -> kotlin.Unit) {
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