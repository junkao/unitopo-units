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

package io.frinx.unitopo.unit.xr7.interfaces.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations._interface.configuration.MtusBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations._interface.configuration.mtus.MtuBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class InterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

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

        ifcCfgBuilder
            .setInterfaceName(ifcName)
            .setActive(interfaceActive)
            .setDescription(dataAfter.description)
        if (dataAfter.type == Ieee8023adLag::class.java) {
            ifcCfgBuilder.setInterfaceVirtual(true)
            if (dataAfter.mtu != null) {
                val owner = CiscoIosXrString("etherbundle")
                val mtu = MtuBuilder().setMtu(dataAfter.mtu.toLong())
                    .setOwner(owner)
                    .build()
                val mtus = MtusBuilder().setMtu(listOf(mtu)).build()
                ifcCfgBuilder.setMtus(mtus).build()
            }
        } else if (dataAfter.type == EthernetCsmacd::class.java) {
            ifcCfgBuilder.apply {
                if (dataAfter.isEnabled) {
                    isShutdown = null
                } else {
                    isShutdown = true
                }
            }
        } else {
            throw WriteFailedException(id, "Interface type " +
                    dataAfter.type.toString() + " is not supported")
        }

        val underlayIfcCfg = ifcCfgBuilder.build()
        return Pair(underlayId, underlayIfcCfg)
    }

    private fun getId(id: InstanceIdentifier<Config>):
        Triple<InterfaceActive, InterfaceName, InstanceIdentifier<InterfaceConfiguration>> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)

        val underlayId = InterfaceReader.IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
        return Triple(interfaceActive, ifcName, underlayId)
    }
}