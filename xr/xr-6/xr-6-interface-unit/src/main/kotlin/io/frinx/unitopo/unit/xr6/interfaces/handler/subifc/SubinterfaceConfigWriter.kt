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

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.ifc.base.handler.subinterfaces.AbstractSubinterfaceConfigWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.Util
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceModeEnum
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class SubinterfaceConfigWriter(underlayAccess: UnderlayAccess) :
    AbstractSubinterfaceConfigWriter<InterfaceConfiguration>(underlayAccess) {

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        if (id.firstKeyOf(Subinterface::class.java).index == Util.ZERO_SUBINTERFACE_ID) {
            return
        }
        super.deleteCurrentAttributes(id, dataBefore, writeContext)
    }

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext) {
        if (id.firstKeyOf(Subinterface::class.java).index == Util.ZERO_SUBINTERFACE_ID) {
            return
        }
        super.writeCurrentAttributes(id, dataAfter, writeContext)
    }

    override fun getIid(id: InstanceIdentifier<Config>): InstanceIdentifier<InterfaceConfiguration> {

        // TODO supporting only "act" interfaces
        val interfaceActive = InterfaceActive("act")

        val underlaySubifcName = InterfaceName(
            getSubIfcName(id.firstKeyOf(Interface::class.java).name,
                id.firstKeyOf(Subinterface::class.java).index))

        return InterfaceReader.IFC_CFGS.child(InterfaceConfiguration::class.java,
            InterfaceConfigurationKey(interfaceActive, underlaySubifcName))
    }

    override fun getData(data: Config, ifcName: String): InterfaceConfiguration {
        val ifcCfgBuilder = InterfaceConfigurationBuilder()
        ifcCfgBuilder.toUnderlay(data, ifcName)
        return ifcCfgBuilder.build()
    }

    private fun InterfaceConfigurationBuilder.toUnderlay(data: Config, ifcName: String) {
        if (data.shutdown()) {
            isShutdown = true
        }
        interfaceName = InterfaceName(getSubIfcName(ifcName, data.index))
        active = InterfaceActive("act")
        interfaceModeNonPhysical = InterfaceModeEnum.Default
        description = data.description
    }

    companion object {
        private fun Config.shutdown() = when (isEnabled) {
            null -> false
            true -> false
            else -> true
        }
    }
}