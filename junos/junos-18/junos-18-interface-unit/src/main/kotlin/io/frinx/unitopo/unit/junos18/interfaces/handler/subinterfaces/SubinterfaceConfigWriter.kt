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

package io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces

import io.frinx.unitopo.ifc.base.handler.subinterfaces.AbstractSubinterfaceConfigWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.extention.rev181204.Config1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.RpmBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.rpm.rpm_or_twamp.Case1Builder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitBuilder as JunosInterfaceUnitBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitKey as JunosInterfaceUnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.enable.disable.Case1Builder as JunosCase1Builder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.rpm.rpm_or_twamp.case_1.rpm.type.Case1Builder as RpmTypeCase1Builder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.rpm.rpm_or_twamp.case_1.rpm.type.Case2Builder as RpmTypeCase2Builder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.rpm.rpm_or_twamp.case_1.rpm.type.Case3Builder as RpmTypeCase3Builder

class SubinterfaceConfigWriter(underlayAccess: UnderlayAccess) :
    AbstractSubinterfaceConfigWriter<JunosInterfaceUnit>(underlayAccess) {

    override fun getData(data: Config, ifcName: String): JunosInterfaceUnit {
        val ifcUnitBuilder = JunosInterfaceUnitBuilder()
        ifcUnitBuilder.toUnderlay(data)
        return ifcUnitBuilder.build()
    }

    override fun getIid(id: InstanceIdentifier<Config>): InstanceIdentifier<JunosInterfaceUnit> {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val unitName = id.firstKeyOf(Subinterface::class.java).index.toString()
        return InterfaceReader.JUNOS_IFCS
            .child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
            .child(JunosInterfaceUnit::class.java, JunosInterfaceUnitKey(unitName))
    }

    private fun JunosInterfaceUnitBuilder.toUnderlay(data: Config) {
        enableDisable = when (data.shutdown()) {
            true -> JunosCase1Builder().setDisable(true).build()
            false -> JunosCase1Builder().setDisable(null).build()
        }
        name = data.index.toString()
        description = data.description
        key = JunosInterfaceUnitKey(name)
        rpm = null // give rpm a initialize value null
        data.getAugmentation(Config1::class.java)?.let {
            this.rpm = RpmBuilder().apply {
                rpmOrTwamp = Case1Builder().apply {
                    rpmType = when (it.rpmType?.intValue) {
                        0 -> RpmTypeCase1Builder().setClient(true).build()
                        1 -> RpmTypeCase2Builder().setServer(true).build()
                        2 -> RpmTypeCase3Builder().setClientDelegateProbes(true).build()
                        else -> null
                    }
                }.build()
            }.build()
        }
    }

    // If isEnabled is null, it determines that subinterface is enabled
    private fun Config.shutdown() = isEnabled == false
}