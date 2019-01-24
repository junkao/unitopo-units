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

package io.frinx.unitopo.unit.xr6.network.instance.vrf.ifc

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.handlers.l3vrf.L3VrfListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.InterfaceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class VrfInterfaceReader(private val underlayAccess: UnderlayAccess) :
    L3VrfListReader.L3VrfConfigListReader<Interface, InterfaceKey, InterfaceBuilder> {

    private val interfaceReader: InterfaceReader = InterfaceReader(underlayAccess)

    override fun readCurrentAttributesForType(
        id: InstanceIdentifier<Interface>,
        builder: InterfaceBuilder,
        ctx: ReadContext
    ) {
        val ifcName = id.firstKeyOf(Interface::class.java).id
        builder.id = ifcName
    }

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<Interface>) {
        (builder as InterfacesBuilder).`interface` = readData
    }

    override fun getBuilder(id: InstanceIdentifier<Interface>): InterfaceBuilder = InterfaceBuilder()

    override fun getAllIdsForType(id: InstanceIdentifier<Interface>, context: ReadContext): List<InterfaceKey> {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name

        val allIfcs = underlayAccess.read(InterfaceReader.IFC_CFGS)
                .checkedGet()
                .orNull()?.interfaceConfiguration ?: emptyList<InterfaceConfiguration>()

        return allIfcs.filter { it.getVrf() == vrfName }
                .map { InterfaceKey(it.interfaceName.value) }
                .toList()
    }
}

fun InterfaceConfiguration.getVrf(): String {
    getAugmentation(InterfaceConfiguration1::class.java)?.let {
        return it.vrf?.value ?: NetworInstance.DEFAULT_NETWORK_NAME
    }

    return NetworInstance.DEFAULT_NETWORK_NAME
}