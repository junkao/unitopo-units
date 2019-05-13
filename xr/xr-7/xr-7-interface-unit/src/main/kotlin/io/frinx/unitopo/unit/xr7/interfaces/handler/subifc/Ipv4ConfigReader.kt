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

package io.frinx.unitopo.unit.xr7.interfaces.handler.subifc

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr7.interfaces.handler.Util
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev180111.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.address.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.address.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class Ipv4ConfigReader(private val underlayAccess: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(id: InstanceIdentifier<Config>): ConfigBuilder = ConfigBuilder()

    override fun readCurrentAttributes(id: InstanceIdentifier<Config>, builder: ConfigBuilder, ctx: ReadContext) {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val ifcIndex = id.firstKeyOf(Subinterface::class.java).index
        val subIfcName = when (ifcIndex) {
            Util.ZERO_SUBINTERFACE_ID -> ifcName
            else -> Util.getSubIfcName(ifcName, ifcIndex)
        }
        builder.ip = id.firstKeyOf(Address::class.java).ip
        InterfaceReader.readInterfaceCfg(underlayAccess, subIfcName) { extractAddress(it, builder) }
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
                    .map { Integer.toBinaryString(it) } // generate 4 binaries here
                    .map { it.occurrences("1").toShort() } // count number of "1" of each binary
                    .reduce { acc, i -> (acc + i).toShort() } // sum up number of "1" to get the length
        }
    }
}

fun String.occurrences(substring: String): Int {
    return this.split(substring).count() - 1
}