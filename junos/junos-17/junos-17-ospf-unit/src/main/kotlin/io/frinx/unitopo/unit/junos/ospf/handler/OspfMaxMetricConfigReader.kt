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
import io.frinx.unitopo.handlers.ospf.OspfReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.timers.MaxMetricBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.timers.max.metric.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.timers.max.metric.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import java.math.BigInteger
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfMaxMetricConfigReader(private val underlayAccess: UnderlayAccess) :
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