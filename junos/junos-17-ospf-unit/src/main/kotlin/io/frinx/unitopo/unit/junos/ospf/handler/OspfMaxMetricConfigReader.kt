/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.handlers.ospf.OspfReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.timers.MaxMetricBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.timers.max.metric.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.timers.max.metric.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import java.math.BigInteger
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfMaxMetricConfigReader(private val underlayAccess: UnderlayAccess):
        OspfReader.OspfConfigReader<Config, ConfigBuilder> {

    override fun getBuilder(id: IID<Config>): ConfigBuilder {
        return ConfigBuilder()
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as MaxMetricBuilder).config = config
    }

    override fun readCurrentAttributesForType(id: IID<Config>, config: ConfigBuilder, readContext: ReadContext) {
        val timeout = getTimeout()

        if (timeout != null) {
            config.isSet = true
            config.timeout = BigInteger.valueOf(timeout)
        }
    }

    private fun getTimeout(): Long? {
        val optOspf = underlayAccess.read(OspfProtocolReader.getOspfId()).checkedGet()
        if (!optOspf.isPresent) {
            return null
        }
        return optOspf.get()?.overload?.timeout?.uint32
    }
}
