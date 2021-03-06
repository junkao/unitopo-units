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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.Util
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.statsd.cfg.rev151109.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.statsd.cfg.rev151109._interface.configurations._interface.configuration.Statistics
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.statsd.cfg.rev151109._interface.configurations._interface.configuration.StatisticsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.statistics.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class SubinterfaceStatisticsConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, wtc: WriteContext) {
        if (id.firstKeyOf(Subinterface::class.java).index == Util.ZERO_SUBINTERFACE_ID) {
            return
        }
        val (underlayId, underlayIfcCfg) = getData(id, dataAfter)
        underlayAccess.merge(underlayId, underlayIfcCfg)
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, wtc: WriteContext) {
        if (id.firstKeyOf(Subinterface::class.java).index == Util.ZERO_SUBINTERFACE_ID) {
            return
        }
        underlayAccess.delete(getId(id))
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        if (dataAfter.loadInterval != null) {
            writeCurrentAttributes(id, dataAfter, writeContext)
        } else {
            deleteCurrentAttributes(id, dataBefore, writeContext)
        }
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
        Pair<InstanceIdentifier<Statistics>, Statistics>
    {
        val underlayIfcCfg = StatisticsBuilder()
                .setLoadInterval(dataAfter.loadInterval)
                .build()
        return Pair(getId(id), underlayIfcCfg)
    }

    private fun getId(id: InstanceIdentifier<Config>): InstanceIdentifier<Statistics> {

        val interfaceActive = InterfaceActive("act")
        val underlaySubifcName = InterfaceName(
                Util.getSubIfcName(id.firstKeyOf(Interface::class.java).name,
                        id.firstKeyOf(Subinterface::class.java).index))

        val underlayId = InterfaceReader.IFC_CFGS
                .child(InterfaceConfiguration::class.java,
                        InterfaceConfigurationKey(interfaceActive, underlaySubifcName))
                .augmentation(InterfaceConfiguration1::class.java)
                .child(Statistics::class.java)
        return underlayId
    }
}