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

package io.frinx.unitopo.unit.xr7.logging.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.interfaces.handler.Util
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405.InterfaceModeEnum
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.event.types.rev171024.LINKUPDOWN
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.interfaces.structural.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class LoggingInterfacesConfigWriter(private val underlayAccess: UnderlayAccess) :
    WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        instanceIdentifier: IID<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        require(dataAfter.interfaceId.value.startsWith("Bundle-Ether")) {
            "${dataAfter.interfaceId.value} Physical interface is not supported"
        }
        val (underlayId, underlayIfcCfg) = getData(instanceIdentifier, dataAfter)
        underlayAccess.put(underlayId, underlayIfcCfg)
    }

    override fun updateCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val underlayId = getId(id)
        val before = underlayAccess.read(underlayId)
            .checkedGet()
            .get()
        val underlayConfig = InterfaceConfigurationBuilder(before)
            .setLinkStatus(null)
            .build()
        underlayAccess.put(underlayId, underlayConfig)
    }

    private fun getData(id: IID<Config>, data: Config):
        Pair<IID<InterfaceConfiguration>, InterfaceConfiguration> {
        val underlayId = getId(id)
        val underlayBefore = underlayAccess.read(underlayId)
            .checkedGet()
            .orNull()
        val ifcCfgBuilder =
            if (underlayBefore != null) InterfaceConfigurationBuilder(underlayBefore) else
                createNewBuilder(data.interfaceId.value)

        ifcCfgBuilder.setLinkStatus(data.getLinkStatus())
        return Pair(underlayId, ifcCfgBuilder.build())
    }

    private fun getId(id: IID<Config>): IID<InterfaceConfiguration> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).interfaceId.value)

        return LoggingInterfacesReader.IFC_CFGS.child(InterfaceConfiguration::class.java,
            InterfaceConfigurationKey(interfaceActive, ifcName))
    }

    companion object {
        private fun Config.hasLinkUpDownEvent(): Boolean {
            return enabledLoggingForEvent.orEmpty()
                .map { it.eventName }
                .contains(LINKUPDOWN::class.java)
        }

        private fun Config.getLinkStatus(): Boolean? {
            if (hasLinkUpDownEvent()) {
                return true
            }
            return null
        }

        private fun createNewBuilder(name: String): InterfaceConfigurationBuilder {
            val ifcCfgBuilder = InterfaceConfigurationBuilder()
                .setActive(InterfaceActive("act"))
                .setInterfaceName(InterfaceName(name))

            if (Util.isSubinterface(name)) {
                ifcCfgBuilder.setInterfaceModeNonPhysical(InterfaceModeEnum.Default)
            } else {
                ifcCfgBuilder.setInterfaceVirtual(true)
            }

            return ifcCfgBuilder
        }
    }
}