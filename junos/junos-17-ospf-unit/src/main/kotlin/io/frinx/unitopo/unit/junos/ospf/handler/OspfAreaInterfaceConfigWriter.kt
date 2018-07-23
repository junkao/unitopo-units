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
import io.frinx.unitopo.handlers.ospf.OspfWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.area.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.area.InterfaceBuilder as JunosInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.area.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfAreaInterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : OspfWriter<Config> {

    override fun updateCurrentAttributesForType(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeCurrentAttributesForType(id, dataAfter, writeContext)
    }

    override fun writeCurrentAttributesForType(id: IID<Config>, dataAfter: Config, writeContext: WriteContext) {
        val (underlayId, underlayIfcCfg) = getData(id, dataAfter)
        try {
            underlayAccess.put(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributesForType(id: IID<Config>, dataBefore: Config, writeContext: WriteContext) {
        val (ifaceIid, iface) = getInterfaceBuilder(id)
        iface.metric = null
        try {
            underlayAccess.put(ifaceIid, iface.build())
        } catch (e: Exception) {
            throw WriteFailedException.DeleteFailedException(id, e)
        }
    }

    private fun getData(id: IID<Config>, dataAfter: Config): Pair<IID<JunosInterface>, JunosInterface> {
        val (ifaceIid, iface) = getInterfaceBuilder(id)
        iface.metric = JunosInterface.Metric(dataAfter.metric?.value)
        return Pair(ifaceIid, iface.build())
    }

    private fun getInterfaceBuilder(id: IID<Config>): Pair<IID<JunosInterface>, JunosInterfaceBuilder> {
        val areaId = String(id.firstKeyOf(Area::class.java).identifier.value)
        val ifaceId = id.firstKeyOf(Interface::class.java).id
        val ifaceIid = OspfProtocolReader.getInterfaceId(areaId, ifaceId)
        val ifaceOrig = underlayAccess.read(ifaceIid).checkedGet()
        return if (ifaceOrig.isPresent) {
            Pair(ifaceIid, JunosInterfaceBuilder(ifaceOrig.get()).setKey(InterfaceKey(JunosInterface.Name(ifaceId))))
        } else {
            Pair(ifaceIid, JunosInterfaceBuilder().setKey(InterfaceKey(JunosInterface.Name(ifaceId))))
        }
    }
}