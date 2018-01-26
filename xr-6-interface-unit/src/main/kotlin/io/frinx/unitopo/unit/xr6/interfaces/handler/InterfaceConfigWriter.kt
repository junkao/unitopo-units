/*
 * Copyright 2017 FRINX s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.frinx.unitopo.unit.xr6.interfaces.handler

import io.fd.honeycomb.translate.read.ReadFailedException
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
        val (underlayId, underlayIfcCfg) = getData(id, dataAfter)

        try {
            underlayAccess.put(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.read.ReadFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, dataBefore: Config, writeContext: WriteContext) {
        val (_, _, underlayId) = getId(id)

        try {
            underlayAccess.delete(underlayId)
        } catch (e: Exception) {
            throw ReadFailedException(id, e)
        }
    }

    override fun updateCurrentAttributes(id: InstanceIdentifier<Config>,
                                         dataBefore: Config, dataAfter: Config,
                                         writeContext: WriteContext) {
        val (underlayId, underlayIfcCfg) = getData(id, dataAfter)

        try {
            val before = underlayAccess.read(underlayId)
                    .checkedGet()
                    .orNull()

            // Check if enabling the interface from disabled state
            // since shutdown is an empty leaf, enabling an interface cannot be done with merge
            if (before != null &&
                    before.isShutdown != null &&
                    dataAfter.isEnabled) {

                val previousStateWithoutShut = InterfaceConfigurationBuilder(before).setShutdown(null).build()
                underlayAccess.put(underlayId, previousStateWithoutShut)
            }

            underlayAccess.merge(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw ReadFailedException(id, e)
        }
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<InterfaceConfiguration>, InterfaceConfiguration> {
        val (interfaceActive, ifcName, underlayId) = getId(id)

        val ifcCfgBuilder = InterfaceConfigurationBuilder()
        if (!dataAfter.isEnabled) ifcCfgBuilder.isShutdown = true
        if (isVirtualInterface(dataAfter.type)) ifcCfgBuilder.isInterfaceVirtual = true

        val underlayIfcCfg = ifcCfgBuilder
                .setInterfaceName(ifcName)
                .setActive(interfaceActive)
                .setDescription(dataAfter.description)
                .build()
        return Pair(underlayId, underlayIfcCfg)
    }

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