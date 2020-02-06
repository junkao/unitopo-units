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

package io.frinx.unitopo.unit.xr7.interfaces.handler.subifc

import io.frinx.unitopo.ifc.base.handler.subinterfaces.AbstractSubinterfaceReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr7.interfaces.handler.Util
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations._interface.configuration.mtus.MtuKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev190405.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class SubinterfaceReader(underlayAccess: UnderlayAccess) :
    AbstractSubinterfaceReader<InterfaceConfigurations>(underlayAccess) {

    private val ifcReader = InterfaceReader(underlayAccess)

    override fun readIid(ifcName: String): InstanceIdentifier<InterfaceConfigurations> = InterfaceReader.IFC_CFGS

    override fun parseSubInterfaceIds(data: InterfaceConfigurations, ifcName: String): List<SubinterfaceKey> {
        val subIfcKeys = ifcReader.getInterfaceIds()
            .filter { Util.isSubinterface(it.name) }
            .filter { it.name.startsWith(ifcName) }
            .map { Util.getSubinterfaceKey(it.name) }

        val ipv4Keys = mutableListOf<AddressKey>()
        InterfaceReader.readInterfaceCfg(underlayAccess, ifcName) { Ipv4AddressReader.extractAddresses(it, ipv4Keys) }

        val mtuKeys = mutableListOf<MtuKey>()
        InterfaceReader.readInterfaceCfg(underlayAccess, ifcName) { extractMtus(it, mtuKeys) }

        return if (ipv4Keys.isNotEmpty() || mtuKeys.isNotEmpty())
            subIfcKeys.plus(SubinterfaceKey(Util.ZERO_SUBINTERFACE_ID)) else
            subIfcKeys
    }

    private fun extractMtus(ifcCfg: InterfaceConfiguration, keys: MutableList<MtuKey>) {
        ifcCfg.getAugmentation(InterfaceConfiguration1::class.java)?.let { interfaceConfiguration1 ->
            interfaceConfiguration1.ipv4Network?.let { ipv4Network ->
                ipv4Network.mtu?.let {
                    keys.add(MtuKey(CiscoIosXrString(it.toString())))
                }
            }
        }
    }
}