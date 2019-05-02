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

import io.frinx.unitopo.ifc.base.handler.AbstractInterfaceReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ip.vrrp.top.vrrp.VrrpGroupKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.root.rev180101.Configuration
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.Configuration1 as IfConfigurationAug
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.Interfaces as JunosInterfaces
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.Address as JunosInterfaceUnitAddress
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.address.VrrpGroup as JunosVrrpGroup

class InterfaceReader(underlayAccess: UnderlayAccess) : AbstractInterfaceReader<JunosInterfaces>(underlayAccess) {

    override fun parseInterfaceIds(data: JunosInterfaces): List<InterfaceKey> =
        data.`interface`.orEmpty()
        .map { InterfaceKey(it.name) }

    override val readIid: InstanceIdentifier<JunosInterfaces> = JUNOS_IFCS

    companion object {
        private val JUNOS_CFG = InstanceIdentifier.create(Configuration::class.java)!!
        private val JUNOS_IFCS_AUG = JUNOS_CFG.augmentation(IfConfigurationAug::class.java)!!
        val JUNOS_IFCS = JUNOS_IFCS_AUG.child(JunosInterfaces::class.java)!!

        fun readInterfaceCfg(underlayAccess: UnderlayAccess, name: String, handler: (JunosInterface) -> Unit) {
            readInterface(underlayAccess, name)
                // Invoke handler with read value or use default
                ?.let { handler(it) }
        }

        private fun readInterface(underlayAccess: UnderlayAccess, name: String): JunosInterface? =
            underlayAccess.read(JUNOS_IFCS.child(JunosInterface::class.java, JunosInterfaceKey(name)),
                LogicalDatastoreType.CONFIGURATION)
                .checkedGet().orNull()

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

        fun readUnitVrrpGroup(
            underlayAccess: UnderlayAccess,
            ifcName: String,
            subIfcId: Long,
            vrrpGroupKey: VrrpGroupKey,
            handler: (JunosVrrpGroup) -> Unit
        ) {
            readInterface(underlayAccess, ifcName)
                    // Invoke handler with read UnitAddress
                    .let {
                        it?.unit?.first {
                            it1 -> it1.name == subIfcId.toString()
                        }?.family?.inet?.address?.map {
                            if (it.vrrpGroup?.size == 0) {
                                null
                            } else {
                                it.vrrpGroup?.filter {
                                    val x: (
                                        v: JunosVrrpGroup
                                    )
                                    -> Boolean = {
                                        it.name.toShort().equals(vrrpGroupKey.virtualRouterId)
                                    }
                                    x(it)
                                }?.get(0)
                            }
                        }?.map {
                            it?.let {
                                handler(it)
                            }
                        }
                    }
        }
    }
}