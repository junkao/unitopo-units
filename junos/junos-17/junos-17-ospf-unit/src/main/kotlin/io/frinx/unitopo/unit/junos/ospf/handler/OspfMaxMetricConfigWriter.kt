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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.timers.max.metric.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.Ospf
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.OspfBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.Overload
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.OverloadBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfMaxMetricConfigWriter(private val underlayAccess: UnderlayAccess) : OspfWriter<Config> {

    override fun writeCurrentAttributesForType(id: IID<Config>, dataAfter: Config, writeContext: WriteContext) {
        writeData(id, dataAfter)
    }

    override fun deleteCurrentAttributesForType(id: IID<Config>, dataBefore: Config, writeContext: WriteContext) {
        val ospfOld = underlayAccess.read(OspfProtocolReader.getOspfId()).checkedGet().get()

        val ospfNew = OspfBuilder(ospfOld)
                .setOverload(OverloadBuilder(ospfOld?.overload)
                        .setTimeout(null)
                        .build())
                .build()

        try {
            underlayAccess.put(OspfProtocolReader.getOspfId(), ospfNew)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun updateCurrentAttributesForType(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeData(id, dataAfter)
    }

    private fun writeData(id: IID<Config>, data: Config) {
        val (underlayId, underlayIfcCfg) = getData(data)

        try {
            underlayAccess.merge(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(dataAfter: Config): Pair<IID<Ospf>, Ospf> {
        val ospf = OspfBuilder()
                .setOverload(OverloadBuilder().setTimeout(Overload.Timeout(dataAfter.timeout.toLong()))
                .build())
                .build()
        return Pair(OspfProtocolReader.getOspfId(), ospf)
    }
}