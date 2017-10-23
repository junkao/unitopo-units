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
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.ospf.common.OspfListReader
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces._interface.StateBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class AreaInterfaceReader(private val access: UnderlayAccess) : OspfListReader<Interface, InterfaceKey, InterfaceBuilder> {

    @Throws(ReadFailedException::class)
    override fun getAllIdsForType(id: InstanceIdentifier<Interface>, context: ReadContext): List<InterfaceKey> {
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val protKey = id.firstKeyOf(Protocol::class.java)
        val areaKey = id.firstKeyOf(Area::class.java)

        return AreaReader.getAreas(access, protKey, vrfKey.name)
                ?.areaAreaId.orEmpty()
                .find { areaKey.identifier.uint32 == it.areaId?.toLong() }
                ?.nameScopes
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
    override fun readCurrentAttributesForType(id: InstanceIdentifier<Interface>, builder: InterfaceBuilder, ctx: ReadContext) {
        val interfaceKey = id.firstKeyOf(Interface::class.java)

        builder.id = interfaceKey.id
        builder.config = ConfigBuilder().setId(interfaceKey.id).build()
        builder.state = StateBuilder().setId(interfaceKey.id).build()
    }
}
