/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.interfaces

import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.Config
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
                    before.isShutdown &&
                    dataAfter.isEnabled) {

                val previousStateWithoutShut = InterfaceConfigurationBuilder(before).setShutdown(false).build()
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