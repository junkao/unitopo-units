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

package io.frinx.unitopo.unit.junos.snmp.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Configuration
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.traps.choice.Traps
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Interfaces
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces.Interface as SnmpInterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces.InterfaceBuilder as SnmpInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces.InterfaceKey as SnmpInterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class SnmpInterfaceReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<SnmpInterface, SnmpInterfaceKey, SnmpInterfaceBuilder> {

    override fun getBuilder(id: IID<SnmpInterface>): SnmpInterfaceBuilder = SnmpInterfaceBuilder()

    override fun merge(builder: Builder<out DataObject>, iface: List<SnmpInterface>) {
        (builder as InterfacesBuilder).`interface` = iface
    }

    override fun getAllIds(id: IID<SnmpInterface>, readContext: ReadContext): List<SnmpInterfaceKey> {
        return readInterfaceIds(underlayAccess).map { it.first }
    }

    override fun readCurrentAttributes(id: IID<SnmpInterface>, builder: InterfaceBuilder, readContext: ReadContext) {
        val ifcId = id.firstKeyOf(SnmpInterface::class.java).interfaceId
        builder.key = SnmpInterfaceKey(ifcId)
    }

    companion object {

        val UNDERLAY_IFC_ID = IID.create(Configuration::class.java).child(Interfaces::class.java)!!

        fun readInterfaceIds(underlayAccess: UnderlayAccess): List<Pair<SnmpInterfaceKey, Boolean>> {
            val readIfcs = underlayAccess.read(UNDERLAY_IFC_ID, LogicalDatastoreType.CONFIGURATION)
                    .checkedGet().orNull()
            return parseInterfaceIds(readIfcs)
        }

        fun parseInterfaceIds(readIfcs: Interfaces?): List<Pair<SnmpInterfaceKey, Boolean>> {
            return readIfcs
                    ?.`interface`
                    ?.asSequence()
                    ?.filter { it.trapsChoice != null }
                    ?.map {
                        SnmpInterfaceKey(InterfaceId(it.name)) to (it.trapsChoice is Traps)
                    }
                    ?.toList().orEmpty()
        }
    }
}