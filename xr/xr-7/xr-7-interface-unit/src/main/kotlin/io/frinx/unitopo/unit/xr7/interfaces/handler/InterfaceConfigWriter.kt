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

package io.frinx.unitopo.unit.xr7.interfaces.handler

import io.frinx.unitopo.ifc.base.handler.AbstractInterfaceConfigWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations._interface.configuration.MtusBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations._interface.configuration.mtus.MtuBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceConfigWriter(underlayAccess: UnderlayAccess) :
    AbstractInterfaceConfigWriter<InterfaceConfiguration>(underlayAccess) {

    override fun getData(data: Config): InterfaceConfiguration {
        val ifcCfgBuilder = InterfaceConfigurationBuilder()
        ifcCfgBuilder.toUnderlay(data)
        return ifcCfgBuilder.build()
    }

    private fun InterfaceConfigurationBuilder.toUnderlay(data: Config) {
        if (data.type != EthernetCsmacd::class.java && data.type != Ieee8023adLag::class.java &&
            data.type != SoftwareLoopback::class.java) {
            throw IllegalArgumentException("Interface type " + data.type.toString() + " is not supported")
        }

        interfaceName = InterfaceName(data.name)
        active = InterfaceActive("act")
        description = data.description
        if (data.shutdown()) {
            isShutdown = true
        } else {
            isShutdown = null
        }
        if (data.type == Ieee8023adLag::class.java) {
            isInterfaceVirtual = true
            if (data.mtu != null) {
                val mtu = MtuBuilder().setMtu(data.mtu.toLong())
                    .setOwner(CiscoIosXrString("etherbundle"))
                    .build()
                mtus = MtusBuilder().setMtu(listOf(mtu)).build()
            }
        }
    }

    private fun Config.shutdown() = isEnabled == null || !isEnabled

    override fun getIid(id: InstanceIdentifier<Config>): InstanceIdentifier<InterfaceConfiguration> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)

        return InterfaceReader.IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
    }
}