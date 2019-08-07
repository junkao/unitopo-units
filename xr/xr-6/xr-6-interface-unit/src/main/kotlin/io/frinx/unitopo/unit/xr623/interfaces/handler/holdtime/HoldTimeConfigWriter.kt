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

package io.frinx.unitopo.unit.xr623.interfaces.handler.holdtime

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.drivers.media.eth.cfg.rev150730.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.drivers.media.eth.cfg.rev150730._interface.configurations._interface.configuration.Ethernet
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.drivers.media.eth.cfg.rev150730._interface.configurations._interface.configuration.ethernet.CarrierDelay
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.drivers.media.eth.cfg.rev150730._interface.configurations._interface.configuration.ethernet.CarrierDelayBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class HoldTimeConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {
    override fun writeCurrentAttributes(
        id: IID<Config>,
        dataAfter: Config,
        wtc: WriteContext
    ) {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        require(HoldTimeConfigReader.isSupportedInterface(ifcName)) { "Unsupported interface: $ifcName" }
        val underlayId = getId(id)
        val builder = CarrierDelayBuilder().fromOpenConfig(dataAfter)
        underlayAccess.safePut(underlayId, builder.build())
    }

    override fun deleteCurrentAttributes(
        id: IID<Config>,
        dataAfter: Config,
        wtc: WriteContext
    ) {
        val underlayId = getId(id)
        val underlayBefore = underlayAccess.read(underlayId, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()!!
        underlayAccess.safeDelete(underlayId, underlayBefore)
    }

    override fun updateCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val underlayId = getId(id)
        val underlayBefore = underlayAccess.read(underlayId, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()!!
        val underlayAfter = CarrierDelayBuilder(underlayBefore).fromOpenConfig(dataAfter).build()
        underlayAccess.safeMerge(underlayId, underlayBefore, underlayId, underlayAfter)
    }

    private fun getId(id: IID<Config>): IID<CarrierDelay> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = id.firstKeyOf(Interface::class.java).name
        return InterfaceReader.IFC_CFGS
            .child(InterfaceConfiguration::class.java,
                    InterfaceConfigurationKey(interfaceActive, InterfaceName(ifcName)))
            .augmentation(InterfaceConfiguration1::class.java)
            .child(Ethernet::class.java)
            .child(CarrierDelay::class.java)
    }

    companion object {
        private fun CarrierDelayBuilder.fromOpenConfig(config: Config?): CarrierDelayBuilder {
            carrierDelayUp = config?.up
            carrierDelayDown = config?.down
            return this
        }
    }
}