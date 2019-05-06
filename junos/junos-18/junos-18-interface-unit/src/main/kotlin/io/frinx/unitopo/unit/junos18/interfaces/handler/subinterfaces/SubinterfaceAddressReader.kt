/*
 * Copyright © 2019 Frinx and others.
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

package io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceReader
import org.apache.commons.net.util.SubnetUtils
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.common.types.rev180101.Ipv4prefix
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitKey as JunosInterfaceUnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.Address as JunosInterfaceUnitAddress

class SubinterfaceAddressReader(private val underlayAccess: UnderlayAccess)
    : ConfigListReaderCustomizer<Address, AddressKey, AddressBuilder> {

    override fun getAllIds(iid: InstanceIdentifier<Address>, context: ReadContext): List<AddressKey> {
        val ifcName = iid.firstKeyOf(Interface::class.java).name
        val unitId = iid.firstKeyOf(Subinterface::class.java).index
        return getSubInterfaceAddressIds(underlayAccess, ifcName, unitId.toString())
    }

    private fun getSubInterfaceAddressIds(underlayAccess: UnderlayAccess, ifcName: String, unitId: String):
            List<AddressKey> {
        val instanceIdentifier = InterfaceReader.JUNOS_IFCS
                .child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
                .child(JunosInterfaceUnit::class.java, JunosInterfaceUnitKey(unitId))

        return underlayAccess.read(instanceIdentifier, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()
                ?.let {
                    parseAddressIds(it) }
                .orEmpty()
    }

    private fun parseAddressIds(it: JunosInterfaceUnit): List<AddressKey> {
        return it.family?.inet?.address.orEmpty().map {
            it.name }.map {
            AddressKey(resolveIpv4Address(it)) }
    }

    override fun readCurrentAttributes(iid: InstanceIdentifier<Address>, builder: AddressBuilder, ctx: ReadContext) {
            val (ifcName, subIfcId, addressKey) = resolveKeys(iid)

            InterfaceReader.readUnitAddress(underlayAccess, ifcName, subIfcId, addressKey,
                    { builder.fromUnderlay(it) })
    }

    private fun AddressBuilder.fromUnderlay(address: JunosInterfaceUnitAddress) {
        key = AddressKey(resolveIpv4Address(address.name))
    }

    private fun resolveKeys(iid: InstanceIdentifier<Address>): Triple<String, Long, AddressKey> {
        val ifcName = iid.firstKeyOf(Interface::class.java).name
        val subIfcId = iid.firstKeyOf(Subinterface::class.java).index
        val addressKey = AddressKey(iid.firstKeyOf(Address::class.java).ip)
        return Triple(ifcName, subIfcId, addressKey)
    }
}
private const val DEFAULT_MASK: Short = 24

internal fun resolveIpv4Address(it: Ipv4prefix): Ipv4AddressNoZone {
    return Ipv4AddressNoZone(SubnetUtils(it.value).info?.address)
}
internal fun resolveIpv4Prefix(prefix: Ipv4prefix): Short {
    val address = resolveIpv4Address(prefix).value
    val prefixLength = SubnetUtils(prefix.value).info?.cidrSignature?.removePrefix("$address/")

    if (prefixLength != null) {
        return prefixLength.toShort()
    }
    return DEFAULT_MASK
}