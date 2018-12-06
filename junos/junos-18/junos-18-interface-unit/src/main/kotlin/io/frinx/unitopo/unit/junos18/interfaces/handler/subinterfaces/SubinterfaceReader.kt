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

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitBuilder as JunosInterfaceUnitBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitKey as JunosInterfaceUnitKey

class SubinterfaceReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Subinterface, SubinterfaceKey, SubinterfaceBuilder> {

    override fun getAllIds(iid: InstanceIdentifier<Subinterface>, context: ReadContext): List<SubinterfaceKey> {
        val ifcName = iid.firstKeyOf(Interface::class.java).name
        return getSubInterfaceIds(underlayAccess, ifcName)
    }

    private fun getSubInterfaceIds(underlayAccess: UnderlayAccess, ifcName: String): List<SubinterfaceKey> {
        val instanceIdentifier = InterfaceReader.JUNOS_IFCS.child(JunosInterface::class.java,
                JunosInterfaceKey(ifcName))

        return underlayAccess.read(instanceIdentifier, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()
                ?.let { parseSubInterfaceIds(it) }.orEmpty()
    }

    private fun parseSubInterfaceIds(it: JunosInterface): List<SubinterfaceKey> {
        return it.unit.orEmpty()
                .map { it.key }
                .map { SubinterfaceKey(it.name?.toLong()) }
                .toList()
    }

    override fun readCurrentAttributes(
        iid: InstanceIdentifier<Subinterface>,
        builder: SubinterfaceBuilder,
        context: ReadContext
    ) {
        val ifcName = iid.firstKeyOf(Interface::class.java).name

        InterfaceReader.readUnitCfg(
                underlayAccess,
                ifcName,
                iid.firstKeyOf(Subinterface::class.java).index
        ) { builder.fromUnderlay(it) }
    }

    override fun merge(builder: Builder<out DataObject>, subinterfaces: List<Subinterface>) {
        (builder as SubinterfacesBuilder).subinterface = subinterfaces
    }

    override fun getBuilder(p0: InstanceIdentifier<Subinterface>): SubinterfaceBuilder {
        return SubinterfaceBuilder()
    }

    companion object {
        private fun SubinterfaceBuilder.fromUnderlay(junosUnit: JunosInterfaceUnit) {
            index = junosUnit.name.toLong()
        }

        fun createBuilderFromExistingInterfaceUnit(
            underlayAccess: UnderlayAccess,
            underlayId: InstanceIdentifier<out JunosInterfaceUnit>
        ): JunosInterfaceUnitBuilder {
            val existingInterface = readInterfaceUnit(underlayAccess, underlayId)
            return if (existingInterface == null) {
                JunosInterfaceUnitBuilder()
            } else {
                JunosInterfaceUnitBuilder(existingInterface)
            }
        }

        fun createBuilderFromExistingInterfaceUnit(
            underlayAccess: UnderlayAccess,
            ifcName: String,
            unitName: String
        ): JunosInterfaceUnitBuilder {
            val underlayId = getUnderlayId(ifcName, unitName)
            return createBuilderFromExistingInterfaceUnit(underlayAccess, underlayId)
        }

        private fun getUnderlayId(ifcName: String, unitName: String): InstanceIdentifier<out JunosInterfaceUnit> {
            return InterfaceReader.JUNOS_IFCS
                .child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
                .child(JunosInterfaceUnit::class.java, JunosInterfaceUnitKey(unitName))
        }

        private fun readInterfaceUnit(
            underlayAccess: UnderlayAccess,
            underlayId: InstanceIdentifier<out JunosInterfaceUnit>
        ): JunosInterfaceUnit? {
            return underlayAccess.read(underlayId, LogicalDatastoreType.CONFIGURATION)
                .checkedGet().orNull()
        }
    }
}