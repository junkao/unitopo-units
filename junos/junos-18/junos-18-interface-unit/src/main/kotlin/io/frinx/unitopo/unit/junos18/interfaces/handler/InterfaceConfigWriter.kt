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

import io.frinx.unitopo.ifc.base.handler.AbstractInterfaceConfigWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceBuilder as JunosInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.enable.disable.Case1Builder as JunosCase1Builder

class InterfaceConfigWriter(underlayAccess: UnderlayAccess) :
    AbstractInterfaceConfigWriter<JunosInterface>(underlayAccess) {

    override fun getData(data: Config): JunosInterface {
        require(isIfaceNameAndTypeValid(data.name, data.type)) {
            "Provided type: ${data.type} doesn't match interface name: ${data.name}"
        }
        val ifcBuilder = JunosInterfaceBuilder()
        ifcBuilder.toUnderlay(data)
        return ifcBuilder.build()
    }

    override fun getIid(id: InstanceIdentifier<Config>): InstanceIdentifier<JunosInterface> {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        return InterfaceReader.JUNOS_IFCS.child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
    }

    private fun JunosInterfaceBuilder.toUnderlay(dataAfter: Config) {
        enableDisable = if (dataAfter.shutdown()) {
            JunosCase1Builder().setDisable(true).build()
        } else {
            JunosCase1Builder().setDisable(null).build()
        }
        name = dataAfter.name
    }

    fun isIfaceNameAndTypeValid(ifcName: String, type: Class<out InterfaceType>?): Boolean {
        val ifcType = Util.parseIfcType(ifcName)
        return ifcType == type
    }

    private fun Config.shutdown() = isEnabled == null || !isEnabled
}