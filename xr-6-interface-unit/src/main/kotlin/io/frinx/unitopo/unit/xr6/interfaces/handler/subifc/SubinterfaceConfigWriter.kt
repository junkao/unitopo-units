/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
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
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class SubinterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, dataBefore: Config, writeContext: WriteContext) {
        val (_, _, underlayId) = getId(id)
        underlayAccess.delete(underlayId)
    }

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext) {
        // TODO What about subinterface .0. Should we treat it differently?
        val (underlayId, underlayIfcCfg) = getData(id, dataAfter)

        underlayAccess.put(underlayId, underlayIfcCfg)
    }

    override fun updateCurrentAttributes(id: InstanceIdentifier<Config>, dataBefore: Config,
                                         dataAfter: Config, writeContext: WriteContext) {
        deleteCurrentAttributes(id, dataBefore, writeContext)
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

    companion object {
        fun isInterfaceVirtual(ifcName : InterfaceName) =
            ifcName.value.startsWith("Loopback") || ifcName.value.startsWith("null")

        private fun Config.shutdown() = isEnabled == null || !isEnabled

        public fun getId(id: InstanceIdentifier<out DataObject>):
                Triple<InterfaceActive, InterfaceName, InstanceIdentifier<InterfaceConfiguration>> {

            // TODO supporting only "act" interfaces
            val interfaceActive = InterfaceActive("act")

            val underlaySubifcName = InterfaceName(
                    getSubIfcName(id.firstKeyOf(Interface::class.java).name, id.firstKeyOf(Subinterface::class.java).index))

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

