/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.area.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.area.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfAreaInterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : OspfWriter<Config> {

    override fun updateCurrentAttributesForType(id: IID<Config>, dataBefore: Config, dataAfter: Config, writeContext: WriteContext) {
        writeCurrentAttributesForType(id, dataAfter, writeContext);
    }

    override fun writeCurrentAttributesForType(id: IID<Config>, dataAfter: Config, writeContext: WriteContext) {
        writeData(id, dataAfter)
    }

    override fun deleteCurrentAttributesForType(id: IID<Config>, dataBefore: Config, writeContext: WriteContext) {
        val areaId = String(id.firstKeyOf(Area::class.java).identifier.value)
        val ifaceId = id.firstKeyOf(Interface::class.java).id
        val ifaceIid = OspfProtocolReader.getInterfaceId(areaId, ifaceId)
        val iface = InterfaceBuilder(underlayAccess.read(ifaceIid).checkedGet().get())
                .setMetric(null)
                .build()

        try {
            underlayAccess.put(ifaceIid, iface)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun writeData(id: IID<Config>, data: Config) {
        val (underlayId, underlayIfcCfg) = getData(id, data)

        try {
            underlayAccess.merge(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(id: IID<Config>, dataAfter: Config): Pair<IID<JunosInterface>, JunosInterface> {
        val areaId = String(id.firstKeyOf(Area::class.java).identifier.value)
        val ifaceId = id.firstKeyOf(Interface::class.java).id

        val iface = InterfaceBuilder()
                .setKey(InterfaceKey(JunosInterface.Name(ifaceId)))
                .setMetric(JunosInterface.Metric(dataAfter.metric.value))
                .build()

        return Pair(OspfProtocolReader.getInterfaceId(areaId, ifaceId), iface)
    }

}