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

package io.frinx.unitopo.unit.xr7.logging.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.interfaces.structural.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class LoggingInterfacesConfigWriter(private val underlayAccess: UnderlayAccess) :
    WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        instanceIdentifier: IID<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (underlayId, underlayIfcCfg) = getData(instanceIdentifier, dataAfter, null)
        underlayAccess.put(underlayId, underlayIfcCfg)
    }

    override fun deleteCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val underlayId = getId(id)
        val before: InterfaceConfiguration? = underlayAccess.read(underlayId)
            .checkedGet()
            .get()
        val underlayConfig = InterfaceConfigurationBuilder(before)
            .setLinkStatus(null)
            .setInterfaceModeNonPhysical(null)
            .build()
        underlayAccess.put(underlayId, underlayConfig)
    }

    fun getData(id: IID<Config>, data: Config, underlayBefore: InterfaceConfiguration?):
        Pair<IID<InterfaceConfiguration>, InterfaceConfiguration> {
        val underlayId = getId(id)
        val ifcCfgBuilder =
            if (underlayBefore != null) InterfaceConfigurationBuilder(underlayBefore) else
                InterfaceConfigurationBuilder()
        ifcCfgBuilder.setInterfaceName(InterfaceName(data.interfaceId.value))
        ifcCfgBuilder.setActive(InterfaceActive("act"))
        ifcCfgBuilder.setInterfaceVirtual(true)
        if (data.interfaceId.value.startsWith("Bundle-Ether")) {
            ifcCfgBuilder.setLinkStatus(true)
        } else {
            throw WriteFailedException(id, data.interfaceId.value + " Physical interface is not supported")
        }

        val underlayIfcCfg = ifcCfgBuilder.build()
        return Pair(underlayId, underlayIfcCfg)
    }

    fun getId(id: IID<Config>):
        IID<InterfaceConfiguration> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).interfaceId.value)
        val IFC_CFGS = IID.create(InterfaceConfigurations::class.java)
        val underlayId = IFC_CFGS.child(InterfaceConfiguration::class.java,
            InterfaceConfigurationKey(interfaceActive, ifcName))
        return underlayId
    }
}