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

package io.frinx.unitopo.unit.xr66.interfaces.handler.subifc

import io.frinx.unitopo.ifc.base.handler.subinterfaces.AbstractSubinterfaceConfigReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr66.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.Util
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.ConfigBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class SubinterfaceConfigReader(underlayAccess: UnderlayAccess) :
    AbstractSubinterfaceConfigReader<InterfaceConfigurations>(underlayAccess) {

    override fun readData(
        data: InterfaceConfigurations?,
        configBuilder: ConfigBuilder,
        ifcName: String,
        subIfcIndex: Long
    ) {
        // subinterface enabled is the same as parent interface enabled
        Util.filterInterface(data, ifcName).let {
            configBuilder.parseEnabled(it ?: Util.getDefaultIfcCfg(ifcName))
        }

        Util.filterInterface(data, Util.getSubIfcName(ifcName, subIfcIndex)).let {
            configBuilder.fromUnderlay(it ?: Util.getDefaultIfcCfg(ifcName), subIfcIndex)
        }
    }

    override fun readIid(ifcName: String, subIfcIndex: Long): InstanceIdentifier<InterfaceConfigurations> =
        InterfaceReader.IFC_CFGS

    private fun ConfigBuilder.fromUnderlay(underlay: InterfaceConfiguration, idx: Long) {
        name = underlay.interfaceName.value
        description = underlay.description
        index = idx
    }

    private fun ConfigBuilder.parseEnabled(underlay: InterfaceConfiguration) {
        isEnabled = underlay.isShutdown == null
    }
}