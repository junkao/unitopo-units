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

package io.frinx.unitopo.unit.xr7.interfaces.handler.subifc.ipv4

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr7.interfaces.handler.Util
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev190405.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev190405._interface.configurations._interface.configuration.Ipv4Network
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev190405._interface.configurations._interface.configuration.Ipv4NetworkBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface

open class Ipv4MtuConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {
    override fun writeCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataAfter: Config,
        wtc: WriteContext
    ) {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        require(Ipv4MtuConfigReader.isSupportedInterface(ifcName)) { "Unsupported interface: $ifcName" }

        val (underlayId, underlayIfcCfg) = getData(id, dataAfter)
        underlayAccess.merge(underlayId, underlayIfcCfg)
    }
    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataAfter: Config,
        wtc: WriteContext
    ) {
        val underlayId = getId(id)
        underlayAccess.read(underlayId, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()
                ?.let {
                    underlayAccess.put(underlayId, Ipv4NetworkBuilder(it).setMtu(null).build())
                }
    }
    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val underlayId = getId(id)
        underlayAccess.read(underlayId, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()
                ?.let {
                    underlayAccess.put(underlayId, Ipv4NetworkBuilder(it).setMtu(dataAfter.mtu?.toLong()).build())
                }
    }
    private fun getData(
        id: InstanceIdentifier<Config>,
        dataAfter: Config
    ): Pair<InstanceIdentifier<Ipv4Network>, Ipv4Network> {
        val underlayId = getId(id)
        val underlayIfcCfg = Ipv4NetworkBuilder()
                .setMtu(dataAfter.mtu.toLong())
                .build()
        return Pair(underlayId, underlayIfcCfg)
    }
    private fun getId(
        id: InstanceIdentifier<Config>
    ): InstanceIdentifier<Ipv4Network> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name).value
        val ifcIndex = id.firstKeyOf(Subinterface::class.java).index
        val subIfcName = when (ifcIndex) {
            Util.ZERO_SUBINTERFACE_ID -> ifcName
            else -> Util.getSubIfcName(ifcName, ifcIndex)
        }
        return InterfaceReader.IFC_CFGS
            .child(InterfaceConfiguration::class.java,
                InterfaceConfigurationKey(interfaceActive, InterfaceName(subIfcName)))
            .augmentation(InterfaceConfiguration1::class.java)
            .child(Ipv4Network::class.java)
    }
}