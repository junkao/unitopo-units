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

package io.frinx.unitopo.unit.junos.interfaces.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Configuration
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptions as JunosAggregatedEtherOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Damping as JunosDamping
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.HoldTime as JunosHoldTime
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetection as JunosBfdLivenessDetection
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.gigether.options.Ieee8023ad as JunosGigEthIeee8023ad
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.Address as JunosInterfaceUnitAddress
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Interfaces as JunosInterfaces
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceBuilder as JunosInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class InterfaceReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

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
        val JUNOS_CFG = IID.create(Configuration::class.java)!!
        val IFCS = JUNOS_CFG.child(JunosInterfaces::class.java)!!

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
                    .map { InterfaceKey(it.name) }
                    .toList()
        }

        private fun interfaceExists(underlayAccess: UnderlayAccess, name: String) =
                getInterfaceIds(underlayAccess).contains(InterfaceKey(name))

        fun createBuilderFromExistingInterface(underlayAccess: UnderlayAccess, name: String): JunosInterfaceBuilder {
            val existingInterface = readInterface(underlayAccess, name)
            return if (existingInterface == null)
                JunosInterfaceBuilder()
            else JunosInterfaceBuilder(existingInterface)
        }

        /**
         * Read interface configuration
         */
        fun readInterfaceCfg(underlayAccess: UnderlayAccess, name: String, handler: (JunosInterface) -> kotlin.Unit) {
            readInterface(underlayAccess, name)
                    // Invoke handler with read value or use default
                    .let { it?.let { it1 -> handler(it1) } }
        }

        fun readInterface(underlayAccess: UnderlayAccess, name: String): JunosInterface? {
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

        fun readDampingCfg(underlayAccess: UnderlayAccess, name: String, handler: (JunosDamping) -> Unit) {
            readInterface(underlayAccess, name)
                    // Invoke handler with read HoldTimeConfig or use default
                    .let { it?.damping?.let { it1 -> handler(it1) } }
        }

        fun readEthernetCfg(underlayAccess: UnderlayAccess, name: String, handler: (JunosGigEthIeee8023ad) -> Unit) {
            readInterface(underlayAccess, name)
                    // Invoke handler with read HoldTimeConfig or use default
                    .let { it?.gigetherOptions?.ieee8023ad?.let { it1 -> handler(it1) } }
        }

        fun readUnitCfg(
            underlayAccess: UnderlayAccess,
            name: String,
            unitId: Long,
            handler: (JunosInterfaceUnit) -> Unit
        ) {
            readInterface(underlayAccess, name)
                    // Invoke handler with read UnitCfg
                    .let { it?.unit?.first { it1 -> it1.name == unitId.toString() }
                            ?.let { it2 -> handler(it2) } }
        }

        fun readUnitAddress(
            underlayAccess: UnderlayAccess,
            ifcName: String,
            subIfcId: Long,
            addressKey: AddressKey,
            handler: (JunosInterfaceUnitAddress) -> Unit
        ) {
            readInterface(underlayAccess, ifcName)
                    // Invoke handler with read UnitAddress
                    .let {
                        it?.unit?.first { it1 -> it1.name == subIfcId.toString() }
                                ?.family?.inet?.address
                            ?.first { address -> address.name.value.contains(addressKey.ip.value) }
                                ?.let { it2 -> handler(it2) }
                    }
        }

        fun readAggregationCfg(
            underlayAccess: UnderlayAccess,
            ifcName: String,
            handler: (JunosAggregatedEtherOptions) -> Unit
        ) {
            readInterface(underlayAccess, ifcName)
                    // Invoke handler with read UnitAddress
                    .let {
                        it?.aggregatedEtherOptions
                                ?.let { it2 -> handler(it2) }
                    }
        }

        fun readAggregationBfdCfg(
            underlayAccess: UnderlayAccess,
            ifcName: String,
            handler: (JunosBfdLivenessDetection) -> Unit
        ) {
            readInterface(underlayAccess, ifcName)
                    // Invoke handler with read UnitAddress
                    .let {
                        it?.aggregatedEtherOptions?.bfdLivenessDetection
                                ?.let { it2 -> handler(it2) }
                    }
        }
    }
}