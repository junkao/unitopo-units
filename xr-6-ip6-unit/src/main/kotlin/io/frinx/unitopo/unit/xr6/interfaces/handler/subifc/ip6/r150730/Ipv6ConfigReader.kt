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
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.AddressBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.address.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.address.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev150730.InterfaceConfiguration1 as UnderlayIpv6Augment

open class Ipv6ConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(id: InstanceIdentifier<Config>): ConfigBuilder = ConfigBuilder()

    override fun readCurrentAttributes(id: InstanceIdentifier<Config>, builder: ConfigBuilder, ctx: ReadContext) {
        // For now, only subinterface with ID ZERO_SUBINTERFACE_ID can have IP
        if (id.firstKeyOf(Subinterface::class.java).index != 0L) {
            return
        }

        val name = id.firstKeyOf(Interface::class.java).name
        builder.ip = id.firstKeyOf(Address::class.java).ip
        Ipv6AddressReader.readInterfaceCfg(underlayAccess, name, getHandler(builder))
    }

    open fun getHandler(builder: ConfigBuilder): (InterfaceConfiguration) -> kotlin.Unit =
            { extractAddress(it, builder) }

    override fun merge(builder: Builder<out DataObject>, readValue: Config) {
        (builder as AddressBuilder).config = readValue
    }

    companion object {
        val LINK_LOCAL_PREFIX: Short = 64

        private fun extractAddress(ifcCfg: InterfaceConfiguration, builder: ConfigBuilder) {
            ifcCfg.getAugmentation(UnderlayIpv6Augment::class.java)
                    ?.ipv6Network
                    ?.addresses
                    ?.let {
                        it.linkLocalAddress?.let {
                            if (builder.ip == it.address.ipv6AddressNoZone) {
                                builder.prefixLength = LINK_LOCAL_PREFIX
                            }
                        }
                        it.regularAddresses
                                ?.regularAddress.orEmpty()
                                .firstOrNull { builder.ip == it.address.ipv6AddressNoZone }
                                ?.let { builder.prefixLength = it.prefixLength.value.toShort() }
                    }
        }
    }
}