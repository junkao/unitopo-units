/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.interfaces.handler.subifc.holdtime

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr7.interfaces.handler.Util
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev190405.InterfaceConfiguration7
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev190405._interface.configurations._interface.configuration.CarrierDelay
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev190405._interface.configurations._interface.configuration.CarrierDelayBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
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
        underlayAccess.merge(underlayId, builder.build())
    }

    override fun deleteCurrentAttributes(
        id: IID<Config>,
        dataAfter: Config,
        wtc: WriteContext
    ) {
        val underlayId = getId(id)
        underlayAccess.read(underlayId, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
            ?.let {
                val builder = CarrierDelayBuilder(it).fromOpenConfig(null)
                underlayAccess.put(underlayId, builder.build())
            }
    }

    override fun updateCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val underlayId = getId(id)
        underlayAccess.read(underlayId, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
            ?.let {
                val builder = CarrierDelayBuilder(it).fromOpenConfig(dataAfter)
                underlayAccess.put(underlayId, builder.build())
            }
    }

    private fun getId(id: IID<Config>): IID<CarrierDelay> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val subifcIndex = id.firstKeyOf(Subinterface::class.java).index
        val subifcName = InterfaceName(Util.getSubIfcName(ifcName, subifcIndex))

        return InterfaceReader.IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, subifcName))
            .augmentation(InterfaceConfiguration7::class.java)
            .child(CarrierDelay::class.java)
    }

    companion object {
        private fun CarrierDelayBuilder.fromOpenConfig(config: Config?): CarrierDelayBuilder {
            carrierDelayUp = config?.up

            // You should set 0 to carrie-delay-down as default-value if you set any value to carrier-delay-up.
            carrierDelayDown = if (carrierDelayUp == null) null else CARRIER_DELAY_DOWN_DEFAULT
            return this
        }

        private const val CARRIER_DELAY_DOWN_DEFAULT = 0L
    }
}