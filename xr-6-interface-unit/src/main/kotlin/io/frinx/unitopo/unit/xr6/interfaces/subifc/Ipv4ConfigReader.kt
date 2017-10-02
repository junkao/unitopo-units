package io.frinx.unitopo.unit.xr6.interfaces.subifc

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev150730.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.Address
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.address.Config
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.address.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class Ipv4ConfigReader(private val underlayAccess: UnderlayAccess) : ReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(id: InstanceIdentifier<Config>): ConfigBuilder = ConfigBuilder()

    override fun readCurrentAttributes(id: InstanceIdentifier<Config>, builder: ConfigBuilder, ctx: ReadContext) {
        val name = id.firstKeyOf(Interface::class.java).name
        builder.ip = id.firstKeyOf(Address::class.java).ip
        InterfaceReader.readInterface(underlayAccess, name, { extractAddress(it, builder) })
    }

    override fun merge(builder: Builder<out DataObject>, readValue: Config) {
        (builder as AddressBuilder).config = readValue
    }

    companion object {
        private fun extractAddress(ifcCfg: InterfaceConfiguration, builder: ConfigBuilder) {
            ifcCfg.getAugmentation(InterfaceConfiguration1::class.java)?.let {
                it.ipv4Network?.let {
                    it.addresses?.let {
                        it.primary?.let {
                            if (builder.ip == it.address) {
                                builder.prefixLength = prefixFromNetmask(it.netmask!!)
                            }
                        }
                        it.secondaries?.let {
                            it.secondary
                                    ?.firstOrNull { builder.ip == it.address }
                                    ?.let {
                                        builder.prefixLength = prefixFromNetmask(it.netmask!!)
                                    }
                        }
                    }
                }
            }
        }

        private fun prefixFromNetmask(ip: Ipv4AddressNoZone): Short? {
            return ip.value.split(".")
                    .map { it.toInt() }
                    .map { Integer.toBinaryString(it) }
                    .map { it.occurrences("1").toShort() }
                    .reduce { acc, i -> (acc + i).toShort() }
        }
    }

}

fun String.occurrences(substring: String) : Int {
    return this.split(substring).count() - 1
}