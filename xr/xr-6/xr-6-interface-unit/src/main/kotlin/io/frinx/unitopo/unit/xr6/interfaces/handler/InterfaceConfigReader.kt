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

import io.frinx.unitopo.ifc.base.handler.AbstractInterfaceConfigReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.Util
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.ConfigBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730._interface.table.interfaces.Interface

class InterfaceConfigReader(underlayAccess: UnderlayAccess) :
    AbstractInterfaceConfigReader<InterfaceConfigurations>(underlayAccess) {

    override fun readIid(ifcName: String): InstanceIdentifier<InterfaceConfigurations> = InterfaceReader.IFC_CFGS

    override fun readData(data: InterfaceConfigurations?, configBuilder: ConfigBuilder, ifcName: String) {
        Util.filterInterface(data, ifcName).let {
                // Invoke handler with read value or use default
                // XR returns no config data for interface that has no configuration but is up
            configBuilder.fromUnderlay(it ?: Util.getDefaultIfcCfg(ifcName))
        }
        InterfaceReader.readInterfaceProps(underlayAccess, ifcName) { configBuilder.fromUnderlay(it) }
    }

    fun ConfigBuilder.fromUnderlay(underlay: InterfaceConfiguration) {
        name = underlay.interfaceName.value
        description = underlay.description
        isEnabled = underlay.isShutdown == null
    }

    fun ConfigBuilder.fromUnderlay(underlay: Interface) {
        type = Util.parseIfcType(underlay.interfaceName.value)
        mtu = underlay.mtu.toInt()
    }
}