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

import io.frinx.unitopo.ifc.base.handler.AbstractInterfaceConfigReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.EnableDisable
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.enable.disable.Case1
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceConfigReader(underlayAccess: UnderlayAccess) :
    AbstractInterfaceConfigReader<Interface>(underlayAccess) {

    override fun readIid(ifcName: String): InstanceIdentifier<Interface> =
        InterfaceReader.IFCS.child(Interface::class.java, InterfaceKey(ifcName))

    override fun readData(data: Interface?, configBuilder: ConfigBuilder, ifcName: String) {
        data?.let { configBuilder.fromUnderlay(it) }
    }

    private fun ConfigBuilder.fromUnderlay(underlay: Interface) {
        val ifcType = Util.parseIfcType(underlay.name)
        name = underlay.name
        description = underlay.description
        type = ifcType
        mtu = underlay.mtu?.uint32?.toInt()
        isEnabled = parseEnableDisable(underlay.enableDisable)
    }

    private fun parseEnableDisable(enableDisable: EnableDisable?): Boolean? {
        return when (enableDisable) {
            null -> true
            is Case1 -> false
            else -> false
        }
    }
}