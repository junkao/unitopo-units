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

package io.frinx.unitopo.unit.xr623.isis.handler.interfaces

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces._interface.InterfaceAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.IsisAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.IsisSubAddressFamily
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.AFITYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.IPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.IPV6
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.MULTICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.SAFITYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._if.global.afi.safi.list.Af
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._if.global.afi.safi.list.AfBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._if.global.afi.safi.list.AfKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class IsisInterfaceAfiSafiReader(private val access: UnderlayAccess)
    : ConfigListReaderCustomizer<Af, AfKey, AfBuilder> {

    override fun getAllIds(id: InstanceIdentifier<Af>, context: ReadContext): List<AfKey> {
        val protKey = id.firstKeyOf(Protocol::class.java)
        val interfaceKey = id.firstKeyOf(Interface::class.java)
        return getInterfaceAfs(access, protKey, interfaceKey)
            ?.interfaceAf.orEmpty()
            .map { AfKey(getAfiType(it.afName), getSafiType(it.safName)) }
            .toList()
    }

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Af>,
        builder: AfBuilder,
        ctx: ReadContext
    ) {
        val AfKey = id.firstKeyOf(Af::class.java)
        builder.afiName = AfKey.afiName
        builder.safiName = AfKey.safiName
    }

    companion object {
        fun getInterfaceAfs(access: UnderlayAccess, protKey: ProtocolKey, interfaceKey: InterfaceKey): InterfaceAfs? {
            return IsisInterfaceReader.getInterfaces(access, protKey)
                ?.`interface`.orEmpty()
                .find {
                    it.interfaceName.value == interfaceKey.interfaceId.value
                }
                ?.interfaceAfs
        }

        fun getAfiType(afName: IsisAddressFamily): Class<out AFITYPE>? {
            return when (afName) {
                IsisAddressFamily.Ipv4 -> {
                    IPV4::class.java
                }
                IsisAddressFamily.Ipv6 -> {
                    IPV6::class.java
                }
                else -> {
                    null
                }
            }
        }

        fun getSafiType(safName: IsisSubAddressFamily): Class<out SAFITYPE>? {
            return when (safName) {
                IsisSubAddressFamily.Unicast -> {
                    UNICAST::class.java
                }
                IsisSubAddressFamily.Multicast -> {
                    MULTICAST::class.java
                }
                else -> {
                    null
                }
            }
        }
    }
}