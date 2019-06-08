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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.structure.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Areaid
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.AreaBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.AreaKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.Area as JunosArea
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfAreaConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun updateCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

    override fun writeCurrentAttributes(id: IID<Config>, dataAfter: Config, writeContext: WriteContext) {
        writeData(id)
    }

    override fun deleteCurrentAttributes(id: IID<Config>, dataBefore: Config, writeContext: WriteContext) {
        val data = getData(id)
        underlayAccess.safeDelete(data.first, data.second)
    }

    private fun writeData(id: IID<Config>) {
        val (underlayId, underlayIfcCfg) = getData(id)
        underlayAccess.merge(underlayId, underlayIfcCfg)
    }

    private fun getData(id: IID<Config>): Pair<IID<JunosArea>, JunosArea> {
        val areaId = String(id.firstKeyOf(Area::class.java).identifier.value)
        val area = AreaBuilder()
            .setKey(AreaKey(Areaid(areaId)))
            .build()
        return Pair(OspfProtocolReader.getAreaId(areaId), area)
    }
}