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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.SubinterfaceReader.Companion.ZERO_SUBINTERFACE_ID
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceModeEnum
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.Config
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class SubinterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        if (id.firstKeyOf(Subinterface::class.java).index == ZERO_SUBINTERFACE_ID) {
            return
        }

        val (_, _, underlayId) = getId(id)
        underlayAccess.delete(underlayId)
    }

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext) {
        if (id.firstKeyOf(Subinterface::class.java).index == ZERO_SUBINTERFACE_ID) {
            return
        }

        val (underlayId, underlayIfcCfg) = getData(id, dataAfter)

        underlayAccess.put(underlayId, underlayIfcCfg)
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        if (id.firstKeyOf(Subinterface::class.java).index == ZERO_SUBINTERFACE_ID) {
            return
        }
        val (underlayId, underlayIfcCfg) = getData(id, dataAfter)
        underlayAccess.merge(underlayId, underlayIfcCfg)
    }

    companion object {
        fun isInterfaceVirtual(ifcName: InterfaceName) =
            ifcName.value.startsWith("Loopback") || ifcName.value.startsWith("null")

        private fun Config.shutdown() = isEnabled == null || !isEnabled

        public fun getId(id: InstanceIdentifier<out DataObject>):
                Triple<InterfaceActive, InterfaceName, InstanceIdentifier<InterfaceConfiguration>> {

            // TODO supporting only "act" interfaces
            val interfaceActive = InterfaceActive("act")

            val underlaySubifcName = InterfaceName(
                    getSubIfcName(id.firstKeyOf(Interface::class.java).name,
                        id.firstKeyOf(Subinterface::class.java).index))

            val underlayId = InterfaceReader.IFC_CFGS.child(InterfaceConfiguration::class.java,
                    InterfaceConfigurationKey(interfaceActive, underlaySubifcName))

            return Triple(interfaceActive, underlaySubifcName, underlayId)
        }

        public fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
                Pair<InstanceIdentifier<InterfaceConfiguration>, InterfaceConfiguration> {
            val (interfaceActive, ifcName, underlayId) = getId(id)

            val ifcCfgBuilder = InterfaceConfigurationBuilder()
            if (dataAfter.shutdown()) ifcCfgBuilder.isShutdown = true

            val underlayIfcCfg = ifcCfgBuilder
                    .setInterfaceName(ifcName)
                    .setActive(interfaceActive)
                    .setInterfaceModeNonPhysical(InterfaceModeEnum.Default)
                    .setDescription(dataAfter.description)
                    .build()
            return Pair(underlayId, underlayIfcCfg)
        }
    }
}