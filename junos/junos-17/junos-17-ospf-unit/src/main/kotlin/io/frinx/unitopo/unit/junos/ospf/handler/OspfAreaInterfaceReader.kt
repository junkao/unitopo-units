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
package io.frinx.unitopo.unit.junos.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.handlers.ospf.OspfListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfAreaInterfaceReader(private val underlayAccess: UnderlayAccess) :
        OspfListReader.OspfConfigListReader<Interface, InterfaceKey, InterfaceBuilder> {

    override fun getBuilder(id: IID<Interface>): InterfaceBuilder = InterfaceBuilder()

    override fun merge(builder: Builder<out DataObject>, interfaces: List<Interface>) {
        (builder as InterfacesBuilder).`interface` = interfaces
    }

    override fun readCurrentAttributesForType(id: IID<Interface>, iface: InterfaceBuilder, readContext: ReadContext) {
        iface.key = InterfaceKey(id.firstKeyOf(Interface::class.java).id)
    }

    override fun getAllIdsForType(id: IID<Interface>, readContext: ReadContext): List<InterfaceKey> {
        val areaId = OspfProtocolReader.getAreaId(String(id.firstKeyOf(Area::class.java).identifier.value))
        val optArea = underlayAccess.read(areaId).checkedGet()
        if (!optArea.isPresent) {
            return emptyList()
        }
        return optArea.get()?.`interface`.orEmpty()
                .map {
                    InterfaceKey(String(it.name.value))
                }
    }
}