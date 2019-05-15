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

import io.frinx.unitopo.ifc.base.handler.AbstractInterfaceConfigWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.InterfacesType
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.enable.disable.Case1Builder
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceBuilder as JunosInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey as JunosInterfaceKey

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
        return InterfaceReader.IFCS.child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
    }

    private fun JunosInterfaceBuilder.toUnderlay(data: Config) {
        enableDisable = if (data.shutdown())
            Case1Builder().setDisable(true).build()
        else
            Case1Builder().setDisable(null).build()
        if (data.mtu != null)
            mtu = InterfacesType.Mtu(data.mtu.toLong())
        else
            mtu = null
        name = data.name
        description = data.description
    }

    private fun isIfaceNameAndTypeValid(ifcName: String, type: Class<out InterfaceType>?): Boolean {
        return when (type) {
            EthernetCsmacd::class.java -> isEthernetCsmaCd(ifcName)
            SoftwareLoopback::class.java -> ifcName.startsWith("lo")
            Ieee8023adLag::class.java -> ifcName.startsWith("ae")
            Other::class.java -> true
            else -> false
        }
    }

    private fun Config.shutdown() = isEnabled == null || !isEnabled

    private fun isEthernetCsmaCd(ifcName: String): Boolean {
        return ifcName.startsWith("em") ||  // Management and internal Ethernet interfaces.
            ifcName.startsWith("et") ||     // 100-Gigabit Ethernet interfaces.
            ifcName.startsWith("fe") ||     // Fast Ethernet interface.
            ifcName.startsWith("fxp") ||    // Management and internal Ethernet interfaces.
            ifcName.startsWith("ge") ||     // Gigabit Ethernet interface.
            ifcName.startsWith("xe") // 10-Gigabit Ethernet interface.
    }
}