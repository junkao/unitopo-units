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
package io.frinx.unitopo.unit.xr7.interfaces.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.statsd.cfg.rev170501.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.statsd.cfg.rev170501._interface.configurations._interface.configuration.Statistics
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.statsd.cfg.rev170501._interface.configurations._interface.configuration.StatisticsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.statistics.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface

open class InterfaceStatisticsConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: org.opendaylight.yangtools.yang.binding.InstanceIdentifier<Config>,
        dataAfter: Config,
        wtc: WriteContext
    ) {
        val (underlayId, underlayIfcCfg) = getData(id, dataAfter)
        underlayAccess.put(underlayId, underlayIfcCfg)
    }

    override fun deleteCurrentAttributes(
        id: org.opendaylight.yangtools.yang.binding.InstanceIdentifier<Config>,
        dataAfter: Config,
        wtc: WriteContext
    ) {
        underlayAccess.delete(getId(id))
    }

    override fun updateCurrentAttributes(
        id: org.opendaylight.yangtools.yang.binding.InstanceIdentifier<Config>,
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

    private fun getData(
        id: org.opendaylight.yangtools.yang.binding.InstanceIdentifier<Config>,
        dataAfter: Config
    ):
        Pair<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<Statistics>, Statistics> {
        val underlayId = getId(id)

        val ifcCfgBuilder = StatisticsBuilder()

        val underlayIfcCfg = ifcCfgBuilder
            .setLoadInterval(dataAfter.loadInterval)
            .build()

        return Pair(underlayId, underlayIfcCfg)
    }

    private fun getId(
        id: org.opendaylight.yangtools.yang.binding.InstanceIdentifier<Config>
    ): org.opendaylight.yangtools.yang.binding.InstanceIdentifier<Statistics> {

        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)

        val underlayId = InterfaceReader.IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
            .augmentation(InterfaceConfiguration1::class.java)
            .child(Statistics::class.java)
        return underlayId
    }
}