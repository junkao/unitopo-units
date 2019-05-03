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

import io.frinx.unitopo.ifc.base.handler.AbstractInterfaceConfigReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.EnableDisable
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface

class InterfaceConfigReader(underlayAccess: UnderlayAccess) :
    AbstractInterfaceConfigReader<JunosInterface>(underlayAccess) {

    override fun readIid(ifcName: String): InstanceIdentifier<JunosInterface> =
        InterfaceReader.JUNOS_IFCS.child(JunosInterface::class.java, InterfaceKey(ifcName))

    override fun readData(data: JunosInterface?, configBuilder: ConfigBuilder, ifcName: String) {
        data?.let { configBuilder.fromUnderlay(it) }
    }

    companion object {

        private fun ConfigBuilder.fromUnderlay(underlay: JunosInterface) {
            val ifcType = Util.parseIfcType(underlay.name)

            name = underlay.name
            type = ifcType
            isEnabled = parseEnableDisable(underlay.enableDisable)
        }

        private fun parseEnableDisable(enableDisable: EnableDisable?): Boolean {
            return when (enableDisable) {
                null -> true
                else -> false
            }
        }
    }
}