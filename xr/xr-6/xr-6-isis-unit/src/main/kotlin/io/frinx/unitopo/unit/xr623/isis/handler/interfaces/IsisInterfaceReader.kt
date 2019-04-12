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
import io.frinx.unitopo.unit.xr623.isis.handler.IsisProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class IsisInterfaceReader(private val access: UnderlayAccess) :
    ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    override fun getAllIds(id: InstanceIdentifier<Interface>, context: ReadContext): List<InterfaceKey> {
        val protKey = id.firstKeyOf(Protocol::class.java)

        return getInterfaces(access, protKey)
                ?.`interface`.orEmpty()
                .map { InterfaceKey(InterfaceId(it.interfaceName.value)) }
                .toList()
    }

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Interface>,
        builder: InterfaceBuilder,
        ctx: ReadContext
    ) {
        val interfaceKey = id.firstKeyOf(Interface::class.java)
        builder.interfaceId = interfaceKey.interfaceId
    }

    companion object {
        fun getInstances(access: UnderlayAccess, protKey: ProtocolKey): Instance? {
            return access.read(IsisProtocolReader.UNDERLAY_ISIS)
                    .checkedGet()
                    .orNull()
                    ?.instance.orEmpty()
                    .find {
                        it.instanceName.value == protKey.name
                    }
        }

        fun getInterfaces(access: UnderlayAccess, protKey: ProtocolKey): Interfaces? {
            return getInstances(access, protKey)?.interfaces
        }
    }
}