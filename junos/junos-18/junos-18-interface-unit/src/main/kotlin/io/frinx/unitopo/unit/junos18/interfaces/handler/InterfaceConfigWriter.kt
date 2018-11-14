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

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceBuilder as JunosInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.enable.disable.Case1Builder as JunosCase1Builder

class InterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        require(isIfaceNameAndTypeValid(ifcName, dataAfter.type)) {
            "Provided type: ${dataAfter.type} doesn't match interface name: $ifcName"
        }
        val (underlayId, underlayIfcCfg) = getData(underlayAccess, id, dataAfter)

        underlayAccess.put(underlayId, underlayIfcCfg)
    }

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val (_, underlayId) = getUnderlayId(id)

        underlayAccess.delete(underlayId)
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        require(dataBefore.type == dataAfter.type) {
            "Changing interface type is not permitted. Before: ${dataBefore.type}, After: ${dataAfter.type}"
        }

        // same as write - preserve existing data and override changed.
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

    companion object {
        @VisibleForTesting
        fun isIfaceNameAndTypeValid(ifcName: String, type: Class<out InterfaceType>?): Boolean {
            val ifcType = InterfaceConfigReader.parseIfcType(ifcName)
            return ifcType == type
        }

        private fun getData(underlayAccess: UnderlayAccess, id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<JunosInterface>, JunosInterface> {

            val (ifcName, underlayId) = getUnderlayId(id)
            val ifcBuilder = InterfaceReader.createBuilderFromExistingInterface(underlayAccess, ifcName)

            ifcBuilder.fromOpenConfig(dataAfter)
            return Pair(underlayId, ifcBuilder.build())
        }

        private fun getUnderlayId(id: InstanceIdentifier<Config>): Pair<String, InstanceIdentifier<JunosInterface>> {
            val ifcName = id.firstKeyOf(Interface::class.java).name
            val underlayId = InterfaceReader.JUNOS_IFCS.child(JunosInterface::class.java, JunosInterfaceKey(ifcName))

            return Pair(ifcName, underlayId)
        }

        private fun JunosInterfaceBuilder.fromOpenConfig(
            dataAfter: Config
        ) {
            if (dataAfter.shutdown()) {
                enableDisable = JunosCase1Builder().setDisable(true).build()
            } else {
                enableDisable = JunosCase1Builder().setDisable(null).build()
            }
            name = dataAfter.name
        }

        private fun Config.shutdown() = isEnabled == null || !isEnabled
    }
}