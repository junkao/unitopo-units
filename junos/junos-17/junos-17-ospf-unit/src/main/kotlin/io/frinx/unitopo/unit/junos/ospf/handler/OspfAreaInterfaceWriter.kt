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

import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.handlers.ospf.OspfListWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.area.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.area.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.area.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfAreaInterfaceWriter(private val underlayAccess: UnderlayAccess) : OspfListWriter<Interface, InterfaceKey> {

    override fun updateCurrentAttributesForType(
        id: IID<Interface>,
        dataBefore: Interface,
        dataAfter: Interface,
        writeContext: WriteContext
    ) {
        writeCurrentAttributesForType(id, dataAfter, writeContext)
    }

    override fun writeCurrentAttributesForType(id: IID<Interface>, dataAfter: Interface, writeContext: WriteContext) {
        writeData(id)
    }

    override fun deleteCurrentAttributesForType(id: IID<Interface>, dataBefore: Interface, writeContext: WriteContext) {
        val areaId = String(id.firstKeyOf(Area::class.java).identifier.value)
        val ifaceId = id.firstKeyOf(Interface::class.java).id

        try {
            underlayAccess.delete(OspfProtocolReader.getInterfaceId(areaId, ifaceId))
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun writeData(id: IID<Interface>) {
        val (underlayId, underlayIfcCfg) = getData(id)

        try {
            underlayAccess.merge(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(id: IID<Interface>): Pair<IID<JunosInterface>, JunosInterface> {
        val areaId = String(id.firstKeyOf(Area::class.java).identifier.value)
        val ifaceId = id.firstKeyOf(Interface::class.java).id

        val iface = InterfaceBuilder()
                .setKey(JunosInterfaceKey(JunosInterface.Name(ifaceId)))
                .build()

        return Pair(OspfProtocolReader.getInterfaceId(areaId, ifaceId), iface)
    }
}