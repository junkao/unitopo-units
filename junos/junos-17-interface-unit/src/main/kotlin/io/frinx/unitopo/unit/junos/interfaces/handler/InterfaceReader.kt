/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.interfaces.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Configuration
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.HoldTime as JunosHoldTime
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.HoldTimeBuilder as JunosHoldTimeBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Interfaces as JunosInterfaces
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceBuilder as JunosInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class InterfaceReader(private val underlayAccess: UnderlayAccess) : ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    @Throws(ReadFailedException::class)
    override fun getAllIds(instanceIdentifier: IID<Interface>, readContext: ReadContext): List<InterfaceKey> {
        try {
            return getInterfaceIds(underlayAccess)
        } catch (e: ReadFailedException) {
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
                interfaceBuilder.name = parseIfcName(instanceIdentifier.firstKeyOf(Interface::class.java).name)
            }
        } catch (e: org.opendaylight.controller.md.sal.common.api.data.ReadFailedException) {
            throw ReadFailedException(instanceIdentifier, e)
        }
    }

    companion object {
        val JUNOS_CFG = IID.create(Configuration::class.java)!!
        val IFCS = JUNOS_CFG.child(JunosInterfaces::class.java)!!
        val LAG_PREFIX = "Bundle-Ether"

        fun interfaceExists(underlayAccess: UnderlayAccess, name: IID<out DataObject>) =
                getInterfaceIds(underlayAccess).contains(name.firstKeyOf(Interface::class.java)!!)

        fun getInterfaceIds(underlayAccess: UnderlayAccess): List<InterfaceKey> {
            return underlayAccess.read(IFCS, LogicalDatastoreType.CONFIGURATION)
                    .checkedGet()
                    .orNull()
                    ?.let { parseInterfaceIds(it) }.orEmpty()
        }

        @VisibleForTesting
        private fun parseInterfaceIds(it: JunosInterfaces): List<InterfaceKey> {
            return it.`interface`.orEmpty()
                    .map { it.key }
                    .map { InterfaceKey(parseIfcName(it.name)) }
                    .toList()
        }

        private fun interfaceExists(underlayAccess: UnderlayAccess, name: String) =
                getInterfaceIds(underlayAccess).contains(InterfaceKey(parseIfcName(name)))

        /**
         * Read interface configuration
         */
        fun readInterfaceCfg(underlayAccess: UnderlayAccess, name: String, handler: (JunosInterface) -> kotlin.Unit) {
            readInterface(underlayAccess, name)
                    // Invoke handler with read value or use default
                    .let { it?.let { it1 -> handler(it1) } }
        }

        private fun readInterface(underlayAccess: UnderlayAccess, name: String): JunosInterface? {
            if (!interfaceExists(underlayAccess, name)) {
                return null
            }
            return underlayAccess.read(IFCS.child(JunosInterface::class.java, JunosInterfaceKey(name)),
                    LogicalDatastoreType.CONFIGURATION)
                    .checkedGet().orNull()
        }

        fun readHoldTimeCfg(underlayAccess: UnderlayAccess, name: String, handler: (JunosHoldTime) -> kotlin.Unit) {
            readInterface(underlayAccess, name)
                    // Invoke handler with read HoldTimeConfig or use default
                    .let { it?.holdTime?.let { it1 -> handler(it1) } }
        }

        fun parseIfcName(ifcName: String): String? {
            return when (parseIfcType(ifcName)) {
                Ieee8023adLag::class.java -> LAG_PREFIX + ifcName
                else -> ifcName
            }
        }
    }
}
