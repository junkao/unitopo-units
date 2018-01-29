/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.ospf.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.timers.max.metric.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.Ospf
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.OspfBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.Overload
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.OverloadBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfMaxMetricConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: IID<Config>, dataAfter: Config, writeContext: WriteContext) {
        writeData(id, dataAfter)
    }

    override fun deleteCurrentAttributes(id: IID<Config>, dataBefore: Config, writeContext: WriteContext) {
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

    override fun updateCurrentAttributes(id: IID<Config>, dataBefore: Config, dataAfter: Config, writeContext: WriteContext) {
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