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

import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r150730.Ipv6ConfigReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.address.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303.InterfaceConfiguration1 as UnderlayIpv6Augment

class Ipv6ConfigReader(underlayAccess: UnderlayAccess) : Ipv6ConfigReader(underlayAccess) {

    override fun getHandler(builder: ConfigBuilder): (InterfaceConfiguration) -> kotlin.Unit =
            { extractAddress(it, builder) }

    companion object {
        private fun extractAddress(ifcCfg: InterfaceConfiguration, builder: ConfigBuilder) {
            // TODO this code is same as for the parent handler, since there is no real change between
            // 150730 revision of ipv6-cfg XR model and the 170303 model
            // The only difference is the yangtools generated type hierarchy and the code cant be reused
            // Think about making the parsing code dynamic to be available for multiple revisions
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