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

package io.frinx.unitopo.unit.junos18.interfaces.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.root.rev180101.Configuration
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.Configuration1 as IfConfigurationAug
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.Interfaces as JunosInterfaces
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceBuilder as JunosInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class InterfaceReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    override fun getAllIds(instanceIdentifier: IID<Interface>, readContext: ReadContext): List<InterfaceKey> {
        return getInterfaceIds(underlayAccess)
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
        val ifName = instanceIdentifier.firstKeyOf(Interface::class.java).name
        require(interfaceExists(underlayAccess, ifName)) {
            "Unknown interface is specified: $ifName"
        }

        interfaceBuilder.name = ifName
    }

    companion object {
        private val JUNOS_CFG = IID.create(Configuration::class.java)!!
        private val JUNOS_IFCS_AUG = JUNOS_CFG.augmentation(IfConfigurationAug::class.java)!!
        val JUNOS_IFCS = JUNOS_IFCS_AUG.child(JunosInterfaces::class.java)!!

        fun getInterfaceIds(underlayAccess: UnderlayAccess): List<InterfaceKey> {
            return underlayAccess.read(JUNOS_IFCS, LogicalDatastoreType.CONFIGURATION)
                    .checkedGet()
                    .orNull()
                    ?.let { parseInterfaceIds(it) }.orEmpty()
        }

        private fun parseInterfaceIds(junosInterfaces: JunosInterfaces): List<InterfaceKey> {
            return junosInterfaces.`interface`.orEmpty()
                    .map { InterfaceKey(it.name) }
                    .toList()
        }

        private fun interfaceExists(underlayAccess: UnderlayAccess, name: String) =
                getInterfaceIds(underlayAccess).contains(InterfaceKey(name))

        fun createBuilderFromExistingInterface(underlayAccess: UnderlayAccess, name: String): JunosInterfaceBuilder {
            val existingInterface = readInterface(underlayAccess, name)
            return if (existingInterface == null) {
                JunosInterfaceBuilder()
            } else {
                JunosInterfaceBuilder(existingInterface)
            }
        }

        /**
         * Read interface configuration
         */
        fun readInterfaceCfg(underlayAccess: UnderlayAccess, name: String, handler: (JunosInterface) -> kotlin.Unit) {
            readInterface(underlayAccess, name)
                // Invoke handler with read value or use default
                ?.let { handler(it) }
        }

        private fun readInterface(underlayAccess: UnderlayAccess, name: String): JunosInterface? {
            return if (!interfaceExists(underlayAccess, name)) {
                null
            } else {
                underlayAccess.read(
                    JUNOS_IFCS.child(JunosInterface::class.java, JunosInterfaceKey(name)),
                    LogicalDatastoreType.CONFIGURATION)
                    .checkedGet().orNull()
            }
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
    }
}