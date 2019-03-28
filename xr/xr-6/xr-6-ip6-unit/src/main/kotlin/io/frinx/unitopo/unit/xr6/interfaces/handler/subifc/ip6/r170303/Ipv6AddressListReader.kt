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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r170303

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r150730.Ipv6AddressReader.Companion.readInterfaceCfg
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.AddressBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class Ipv6AddressListReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Address, AddressKey, AddressBuilder> {

    override fun readCurrentAttributes(id: InstanceIdentifier<Address>, builder: AddressBuilder, ctx: ReadContext) {
        builder.ip = id.firstKeyOf(Address::class.java).ip
    }

    override fun getAllIds(id: InstanceIdentifier<Address>, context: ReadContext): MutableList<AddressKey> {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val ifcIndex = id.firstKeyOf(Subinterface::class.java).index
        val subIfcName = when (ifcIndex) {
            Ipv6AddressListReader.ZERO_SUBINTERFACE_ID -> ifcName
            else -> Ipv6AddressListReader.getSubIfcName(ifcName, ifcIndex)
        }
        val keys = mutableListOf<AddressKey>()
        readInterfaceCfg(underlayAccess, subIfcName, { extractAddresses(it, keys) })
        return keys
    }

    companion object {
        fun extractAddresses(ifcCfg: InterfaceConfiguration, keys: MutableList<AddressKey>) {
            ifcCfg.getAugmentation(InterfaceConfiguration1::class.java)?.let {
                    it?.ipv6Network
                    ?.addresses
                    ?.let {
                        it.linkLocalAddress?.let { keys.add(AddressKey(it.address.ipv6AddressNoZone)) }
                        it.regularAddresses
                                ?.regularAddress.orEmpty()
                                .forEach { keys.add(AddressKey(it.address.ipv6AddressNoZone)) }
                    }
            }
        }

        const val ZERO_SUBINTERFACE_ID = 0L
        fun getSubIfcName(ifcName: String, subifcIdx: Long) = ifcName + "." + subifcIdx
    }
}