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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.timers.max.metric.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.Overload
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.OverloadBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfMaxMetricConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: IID<Config>, dataAfter: Config, writeContext: WriteContext) {
        writeData(id, dataAfter)
    }

    override fun deleteCurrentAttributes(id: IID<Config>, dataBefore: Config, writeContext: WriteContext) {
        val data = getOverloadData(dataBefore)
        underlayAccess.safeDelete(data.first, data.second)
    }

    override fun updateCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeData(id, dataAfter)
    }

    private fun writeData(id: IID<Config>, data: Config) {
        val (underlayId, underlayIfcCfg) = getOverloadData(data)
        underlayAccess.merge(underlayId, underlayIfcCfg)
    }

    companion object {
        private fun getOverloadData(data: Config): Pair<IID<Overload>, Overload> {
            val ospf = OverloadBuilder()
                .setTimeout(Overload.Timeout(data.timeout.toLong()))
                .build()
            return Pair(OspfProtocolReader.getOspfOverloadId(), ospf)
        }
    }
}