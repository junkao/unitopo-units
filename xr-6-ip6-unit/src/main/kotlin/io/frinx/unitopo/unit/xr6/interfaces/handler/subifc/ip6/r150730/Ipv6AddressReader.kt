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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r150730

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.AddressesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.AddressBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev150730.InterfaceConfiguration1 as UnderlayIpv6Augment

open class Ipv6AddressReader(private val underlayAccess: UnderlayAccess) : ConfigListReaderCustomizer<Address, AddressKey, AddressBuilder> {

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<Address>) {
        (builder as AddressesBuilder).address = readData
    }

    override fun getBuilder(id: InstanceIdentifier<Address>): AddressBuilder = AddressBuilder()

    override fun readCurrentAttributes(id: InstanceIdentifier<Address>, builder: AddressBuilder, ctx: ReadContext) {
        // For now, only subinterface with ID ZERO_SUBINTERFACE_ID can have IP
        if (id.firstKeyOf(Subinterface::class.java).index != 0L) {
            return
        }

        builder.ip = id.firstKeyOf(Address::class.java).ip
    }

    override fun getAllIds(id: InstanceIdentifier<Address>, context: ReadContext): MutableList<AddressKey> {
        val name = id.firstKeyOf(Interface::class.java).name

        // Getting all configurations and filtering here due to:
        //  - interfaces in underlay are keyed by: name + state compared to only ifc name in openconfig models
        //  - the read is performed in multiple places and with caching its for free
        val keys = mutableListOf<AddressKey>()
        InterfaceReader.readInterfaceCfg(underlayAccess, name, getHandler(keys))
        return keys
    }

    open fun getHandler(keys: MutableList<AddressKey>): (InterfaceConfiguration) -> kotlin.Unit =
            { extractAddresses(it, keys) }

    private fun extractAddresses(ifcCfg: InterfaceConfiguration, keys: MutableList<AddressKey>) {
        ifcCfg.getAugmentation(UnderlayIpv6Augment::class.java)
                ?.ipv6Network
                ?.addresses
                ?.let {
                    it.linkLocalAddress?.let { keys.add(AddressKey(it.address.ipv6AddressNoZone)) }
                    it.regularAddresses
                            ?.regularAddress.orEmpty()
                            .forEach { keys.add(AddressKey(it.address.ipv6AddressNoZone)) }
                }
    }

}