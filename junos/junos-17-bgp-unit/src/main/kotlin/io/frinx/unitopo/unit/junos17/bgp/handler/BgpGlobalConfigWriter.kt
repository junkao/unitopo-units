/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos17.bgp.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpProtocolReader.Companion.UNDERLAY_RT_OPT_AS
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedException
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.routing.options.AutonomousSystem as JunosAutonomousSystem
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.routing.options.AutonomousSystemBuilder as JunosAutonomousSystemBuilder

class BgpGlobalConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext) {
        val underlayIfcCfg = getData(dataAfter)

        try {
            underlayAccess.put(UNDERLAY_RT_OPT_AS, underlayIfcCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>,
                                         dataBefore: Config,
                                         writeContext: WriteContext) {
        try {
            underlayAccess.delete(UNDERLAY_RT_OPT_AS)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun updateCurrentAttributes(id: InstanceIdentifier<Config>,
                                         dataBefore: Config, dataAfter: Config,
                                         writeContext: WriteContext) {
        val autonomousSystem = getData(dataAfter)

        try {
            underlayAccess.merge(UNDERLAY_RT_OPT_AS, autonomousSystem)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(dataAfter: Config):
            JunosAutonomousSystem {
        val builder = JunosAutonomousSystemBuilder()
        builder.asNumber = dataAfter.`as`?.value?.toString()
        return builder.build()
    }
}
