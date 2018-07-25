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
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r150730.Ipv6AddressReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303.InterfaceConfiguration1 as UnderlayIpv6Augment

class Ipv6AddressReader(underlayAccess: UnderlayAccess) : Ipv6AddressReader(underlayAccess) {

    override fun getHandler(keys: MutableList<AddressKey>): (InterfaceConfiguration) -> kotlin.Unit =
            { extractAddresses(it, keys) }

    companion object {
        fun extractAddresses(ifcCfg: InterfaceConfiguration, keys: MutableList<AddressKey>) {
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
}