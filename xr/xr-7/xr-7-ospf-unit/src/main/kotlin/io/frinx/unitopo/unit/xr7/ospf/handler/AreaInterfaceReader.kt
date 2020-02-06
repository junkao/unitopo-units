/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.area.table.AreaAddresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.area.table.area.addresses.area.content.NameScopes
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.AreaKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class AreaInterfaceReader(private val access: UnderlayAccess)
    : ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    @Throws(ReadFailedException::class)
    override fun getAllIds(id: InstanceIdentifier<Interface>, context: ReadContext): List<InterfaceKey> {
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val protKey = id.firstKeyOf(Protocol::class.java)
        val areaKey = id.firstKeyOf(Area::class.java)

        val areas = OspfAreaReader.getAreas(access, protKey, vrfKey.name)
        return findAreaNameScopes(areas, areaKey)
                ?.nameScope.orEmpty()
                .map { InterfaceKey(it.interfaceName.value) }
                .toList()
    }

    override fun merge(builder: Builder<out DataObject>, readData: List<Interface>) {
        (builder as InterfacesBuilder).`interface` = readData
    }

    override fun getBuilder(id: InstanceIdentifier<Interface>): InterfaceBuilder {
        return InterfaceBuilder()
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(
        id: InstanceIdentifier<Interface>,
        builder: InterfaceBuilder,
        ctx: ReadContext
    ) {
        val interfaceKey = id.firstKeyOf(Interface::class.java)
        builder.id = interfaceKey.id
    }

    companion object {
        fun findAreaNameScopes(areas: AreaAddresses?, areaKey: AreaKey): NameScopes? {
            return if (areaKey.identifier.uint32 != null) {
                areas?.areaAreaId.orEmpty()
                        .find { areaKey.identifier.uint32 == it.areaId?.toLong() }
                        ?.nameScopes
            } else {
                areas?.areaAddress.orEmpty()
                        .find { areaKey.identifier.dottedQuad.value == it.address?.value }
                        ?.nameScopes
            }
        }
    }
}