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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitBuilder as JunosInterfaceUnitBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitKey as JunosInterfaceUnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.enable.disable.Case1Builder as JunosCase1Builder

class SubinterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {
    override fun writeCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (underlayIfcUnitId, underlayIfcUnit) = getData(underlayAccess, id, dataAfter)
        underlayAccess.put(underlayIfcUnitId, underlayIfcUnit)
    }

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val (_, _, underlayIfcUnitId) = getUnderlayId(id)
        underlayAccess.delete(underlayIfcUnitId)
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

    companion object {
        private val UNIT_IS_ENABLED = JunosCase1Builder().setDisable(null).build()
        private val UNIT_IS_DISABLED = JunosCase1Builder().setDisable(true).build()

        private fun getData(underlayAccess: UnderlayAccess, id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<JunosInterfaceUnit>, JunosInterfaceUnit> {

            val (_, _, underlayIfcUnitId) = getUnderlayId(id)
            val ifcUnitBuilder = SubinterfaceReader
                .createBuilderFromExistingInterfaceUnit(underlayAccess, underlayIfcUnitId)

            ifcUnitBuilder.fromOpenConfig(dataAfter)
            return Pair(underlayIfcUnitId, ifcUnitBuilder.build())
        }

        private fun getUnderlayId(id: InstanceIdentifier<Config>):
            Triple<String, String, InstanceIdentifier<JunosInterfaceUnit>> {

            val ifcName = id.firstKeyOf(Interface::class.java).name
            val unitName = id.firstKeyOf(Subinterface::class.java).index.toString()
            val underlayIfcUnitId = InterfaceReader.JUNOS_IFCS
                .child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
                .child(JunosInterfaceUnit::class.java, JunosInterfaceUnitKey(unitName))

            return Triple(ifcName, unitName, underlayIfcUnitId)
        }

        private fun JunosInterfaceUnitBuilder.fromOpenConfig(dataAfter: Config) {
            if (dataAfter.shutdown()) {
                enableDisable = UNIT_IS_DISABLED
            } else {
                enableDisable = UNIT_IS_ENABLED
            }
            name = dataAfter.index.toString()
            description = dataAfter.description
            key = JunosInterfaceUnitKey(name)
        }

        // If isEnabled is null, it determines that subinterface is enabled
        private fun Config.shutdown() = isEnabled == false
    }
}