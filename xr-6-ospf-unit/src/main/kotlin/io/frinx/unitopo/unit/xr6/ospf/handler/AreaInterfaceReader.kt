/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.unitopo.handlers.ospf.OspfListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.AreaAddresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.area.addresses.area.content.NameScopes
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

class AreaInterfaceReader(private val access: UnderlayAccess) : OspfListReader.OspfConfigListReader<Interface, InterfaceKey, InterfaceBuilder> {

    @Throws(ReadFailedException::class)
    override fun getAllIdsForType(id: InstanceIdentifier<Interface>, context: ReadContext): List<InterfaceKey> {
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val protKey = id.firstKeyOf(Protocol::class.java)
        val areaKey = id.firstKeyOf(Area::class.java)

        val areas = OspfAreaReader.getAreas(access, protKey, vrfKey.name)
        return findAreaNameScopes(areas, areaKey)
                ?.nameScope.orEmpty()
                .map { InterfaceKey(it.interfaceName.value) }
                .toList()
    }

    private fun findAreaNameScopes(areas: AreaAddresses?, areaKey: AreaKey): NameScopes? {
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

    override fun merge(builder: Builder<out DataObject>, readData: List<Interface>) {
        (builder as InterfacesBuilder).`interface` = readData
    }

    override fun getBuilder(id: InstanceIdentifier<Interface>): InterfaceBuilder {
        return InterfaceBuilder()
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributesForType(id: InstanceIdentifier<Interface>, builder: InterfaceBuilder, ctx: ReadContext) {
        val interfaceKey = id.firstKeyOf(Interface::class.java)
        builder.id = interfaceKey.id
    }
}
