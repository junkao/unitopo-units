package io.frinx.unitopo.unit.xr6.interfaces.subifc

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev150730.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.AddressesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.Address
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.AddressBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class Ipv6AddressReader(private val underlayAccess: UnderlayAccess) : ListReaderCustomizer<Address, AddressKey, AddressBuilder> {

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<Address>) {
        (builder as AddressesBuilder).address = readData
    }

    override fun getBuilder(id: InstanceIdentifier<Address>): AddressBuilder = AddressBuilder()

    override fun readCurrentAttributes(id: InstanceIdentifier<Address>, builder: AddressBuilder, ctx: ReadContext) {
        builder.ip = id.firstKeyOf(Address::class.java).ip
    }

    override fun getAllIds(id: InstanceIdentifier<Address>, context: ReadContext): MutableList<AddressKey> {
        val name = id.firstKeyOf(Interface::class.java).name

        // Getting all configurations and filtering here due to:
        //  - interfaces in underlay are keyed by: name + state compared to only ifc name in openconfig models
        //  - the read is performed in multiple places and with caching its for free
        val keys = mutableListOf<AddressKey>()
        InterfaceReader.readInterface(underlayAccess, name, { extractAddresses(it, keys) })
        return keys
    }

    private fun extractAddresses(ifcCfg: InterfaceConfiguration, keys: MutableList<AddressKey>) {
        ifcCfg.getAugmentation(InterfaceConfiguration1::class.java)?.let {
            it.ipv6Network?.let {
                it.addresses?.let {
                    it.linkLocalAddress?.let {
                        keys.add(AddressKey(it.address.ipv6AddressNoZone))
                    }

                    it.regularAddresses?.let {
                        it.regularAddress?.forEach { keys.add(AddressKey(it.address.ipv6AddressNoZone)) }
                    }

//                     TODO EUI 64
//                    it.eui64Addresses?.let {
//                        it.eui64Address.forEach { keys.add(AddressKey(it.address.ipv6AddressNoZone)) }
//                    }
                }
            }
        }
    }

}