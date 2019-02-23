/*
 * Copyright © 2019 Frinx and others.
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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.statsd.cfg.rev151109.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.statsd.cfg.rev151109._interface.configurations._interface.configuration.Statistics
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.statistics.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.statistics.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.StatisticsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class SubinterfaceStatisticsConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<Config>): ConfigBuilder {
        return ConfigBuilder()
    }

    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val ifcName = instanceIdentifier.firstKeyOf(Interface::class.java).name
        val subifcIndex = instanceIdentifier.firstKeyOf(Subinterface::class.java).index

        // Only parse configuration for non 0 subifc
        if (subifcIndex == SubinterfaceReader.ZERO_SUBINTERFACE_ID) {
            return
        }

        val subifcName = SubinterfaceReader.getSubIfcName(ifcName, subifcIndex)
        InterfaceReader.readInterfaceCfg(underlayAccess, subifcName, { extractStatistics(it, configBuilder) })
    }

    private fun extractStatistics(ifcCfg: InterfaceConfiguration, builder: ConfigBuilder) {
        ifcCfg.getAugmentation(InterfaceConfiguration1::class.java)?.let {
            it.statistics?.let {
                builder.fromUnderlay(it)
            }
        }
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as StatisticsBuilder).config = config
    }
}

private fun ConfigBuilder.fromUnderlay(statictics: Statistics) {
    loadInterval = statictics.loadInterval
}