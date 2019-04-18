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

package io.frinx.unitopo.ni.base.handler.vrf.ifc

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

abstract class AbstractVrfInterfaceReader(protected val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Interface>,
        builder: InterfaceBuilder,
        ctx: ReadContext
    ) {
        val ifcName = id.firstKeyOf(Interface::class.java).id
        builder.id = ifcName
    }

    override fun getAllIds(id: InstanceIdentifier<Interface>, context: ReadContext): List<InterfaceKey> {
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
            return emptyList()
        }
        return getAllInterfaces(vrfKey.name).map { InterfaceKey(it) }
    }

    abstract fun getAllInterfaces(vrfName: String): List<String>
}