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

package io.frinx.unitopo.unit.xr6.interfaces.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext) {
        val (underlayId, underlayIfcCfg) = getData(id, dataAfter, null)

        underlayAccess.put(underlayId, underlayIfcCfg)
    }

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val (_, _, underlayId) = getId(id)

        underlayAccess.delete(underlayId)
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (_, _, underlayId) = getId(id)
        val before = underlayAccess.read(underlayId)
                .checkedGet()
                .orNull()

        val (_, underlayIfcCfg) = getData(id, dataAfter, before)

        underlayAccess.put(underlayId, underlayIfcCfg)
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config, underlayBefore: InterfaceConfiguration?):
            Pair<InstanceIdentifier<InterfaceConfiguration>, InterfaceConfiguration> {
        val (interfaceActive, ifcName, underlayId) = getId(id)

        val ifcCfgBuilder =
                if (underlayBefore != null) InterfaceConfigurationBuilder(underlayBefore) else
                    InterfaceConfigurationBuilder()

        if (dataAfter.shutdown()) ifcCfgBuilder.isShutdown = true else
            ifcCfgBuilder.isShutdown = null

        if (isVirtualInterface(dataAfter.type)) ifcCfgBuilder.isInterfaceVirtual = true

        val underlayIfcCfg = ifcCfgBuilder
                .setInterfaceName(ifcName)
                .setActive(interfaceActive)
                .setDescription(dataAfter.description)
                .setInterfaceModeNonPhysical(null)
                .build()

        return Pair(underlayId, underlayIfcCfg)
    }

    private fun Config.shutdown() = isEnabled == null || !isEnabled

    private fun getId(id: InstanceIdentifier<Config>):
            Triple<InterfaceActive, InterfaceName, InstanceIdentifier<InterfaceConfiguration>> {
        // TODO supporting only "act" interfaces

        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)

        val underlayId = InterfaceReader.IFC_CFGS
                .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
        return Triple(interfaceActive, ifcName, underlayId)
    }

    companion object {
        private fun isVirtualInterface(type: Class<out InterfaceType>): Boolean {
            return type == SoftwareLoopback::class.java
        }
    }
}