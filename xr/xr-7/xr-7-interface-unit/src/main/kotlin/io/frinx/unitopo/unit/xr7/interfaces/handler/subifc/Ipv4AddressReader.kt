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

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr7.interfaces.handler.Util
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.AddressesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev190405.InterfaceConfiguration1 as Ipv4IfcAugment

class Ipv4AddressReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Address, AddressKey, AddressBuilder> {

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<Address>) {
        (builder as AddressesBuilder).address = readData
    }

    override fun getBuilder(id: InstanceIdentifier<Address>): AddressBuilder = AddressBuilder()

    override fun readCurrentAttributes(id: InstanceIdentifier<Address>, builder: AddressBuilder, ctx: ReadContext) {
        builder.ip = id.firstKeyOf(Address::class.java).ip
    }

    override fun getAllIds(id: InstanceIdentifier<Address>, context: ReadContext): MutableList<AddressKey> {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val ifcIndex = id.firstKeyOf(Subinterface::class.java).index
        val subIfcName = when (ifcIndex) {
            Util.ZERO_SUBINTERFACE_ID -> ifcName
            else -> Util.getSubIfcName(ifcName, ifcIndex)
        }
        val keys = mutableListOf<AddressKey>()
        InterfaceReader.readInterfaceCfg(underlayAccess, subIfcName) { extractAddresses(it, keys) }
        return keys
    }

    companion object {
        fun extractAddresses(ifcCfg: InterfaceConfiguration, keys: MutableList<AddressKey>) {
            ifcCfg.getAugmentation(Ipv4IfcAugment::class.java)?.let {
                it.ipv4Network?.let {
                    it.addresses?.let {
                        it.primary?.let {
                            keys.add(AddressKey(it.address))
                        }
                    }
                }
            }
        }
    }
}