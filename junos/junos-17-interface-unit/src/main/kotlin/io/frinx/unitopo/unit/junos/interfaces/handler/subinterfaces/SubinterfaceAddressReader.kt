/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.interfaces.handler.subinterfaces

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import org.apache.commons.net.util.SubnetUtils
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.AddressesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Ipv4prefix
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedException
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.UnitKey as JunosInterfaceUnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.Address as JunosInterfaceUnitAddress
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Interfaces as JunosInterfaces
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey as JunosInterfaceKey

class SubinterfaceAddressReader(private val underlayAccess: UnderlayAccess) : ConfigListReaderCustomizer<Address, AddressKey, AddressBuilder> {

    @Throws(ReadFailedException::class)
    override fun getAllIds(iid: InstanceIdentifier<Address>, context: ReadContext): List<AddressKey> {
        val ifcName = iid.firstKeyOf(Interface::class.java).name
        val unitId = iid.firstKeyOf(Subinterface::class.java).index
        try {
            return getSubInterfaceAddressIds(underlayAccess, ifcName, unitId.toString())
        } catch (e: MdSalReadFailedException) {
            throw ReadFailedException(iid, e)
        }
    }

    private fun getSubInterfaceAddressIds(underlayAccess: UnderlayAccess, ifcName: String, unitId: String): List<AddressKey> {
        val instanceIdentifier = InterfaceReader.IFCS.child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
                .child(JunosInterfaceUnit::class.java, JunosInterfaceUnitKey(unitId))

        return underlayAccess.read(instanceIdentifier, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()
                ?.let { parseAddressIds(it) }.orEmpty()
    }

    private fun parseAddressIds(it: JunosInterfaceUnit): List<AddressKey> {
        return it.family?.inet?.address.orEmpty().map { it.name }.map { AddressKey(resolveIpv4Address(it)) }.toList()
    }

    override fun readCurrentAttributes(iid: InstanceIdentifier<Address>, builder: AddressBuilder, ctx: ReadContext) {
        try {
            val (ifcName, subIfcId, addressKey) = resolveKeys(iid)

            InterfaceReader.readUnitAddress(underlayAccess, ifcName, subIfcId, addressKey, { builder.fromUnderlay(it) })

        } catch (e: MdSalReadFailedException) {
            throw ReadFailedException(iid, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, addresses: List<Address>) {
        (builder as AddressesBuilder).address = addresses
    }

    override fun getBuilder(iid: InstanceIdentifier<Address>): AddressBuilder {
        return AddressBuilder()
    }

}
private const val DEFAULT_MASK: Short = 24

private fun AddressBuilder.fromUnderlay(address: JunosInterfaceUnitAddress) {
    key = AddressKey(resolveIpv4Address(address.name))
}

internal fun resolveKeys(iid: InstanceIdentifier<Address>): Triple<String, Long, AddressKey> {
    val ifcName = iid.firstKeyOf(Interface::class.java).name
    val subIfcId = iid.firstKeyOf(Subinterface::class.java).index
    val addressKey = AddressKey(iid.firstKeyOf(Address::class.java).ip)

    return Triple(ifcName, subIfcId, addressKey)
}

internal fun resolveIpv4Address(it: Ipv4prefix): Ipv4AddressNoZone =
        Ipv4AddressNoZone(SubnetUtils(it.value).info?.address)

internal fun resolveIpv4Prefix(prefix: Ipv4prefix): Short {
    val address = resolveIpv4Address(prefix).value
    val prefixLength = SubnetUtils(prefix.value).info?.cidrSignature?.removePrefix(address + "/")

    if (prefixLength != null) {
        return prefixLength.toShort()
    }
    return DEFAULT_MASK
}
