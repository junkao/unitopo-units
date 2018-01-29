/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.ospf.common.OspfReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.Area as JunosArea
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfAreaInterfaceReader(private val underlayAccess: UnderlayAccess):
        OspfReader.OspfConfigListReader<Interface, InterfaceKey, InterfaceBuilder> {

    override fun getBuilder(id: IID<Interface>): InterfaceBuilder = InterfaceBuilder()

    override fun merge(builder: Builder<out DataObject>, interfaces: List<Interface>) {
        (builder as InterfacesBuilder).`interface` = interfaces
    }

    override fun readCurrentAttributesForType(id: IID<Interface>, iface: InterfaceBuilder, readContext: ReadContext) {
        iface.key = InterfaceKey(id.firstKeyOf(Interface::class.java).id)
    }

    override fun getAllIds(id: IID<Interface>, readContext: ReadContext): List<InterfaceKey> {
        val id = OspfProtocolReader.getAreaId(String(id.firstKeyOf(Area::class.java).identifier.value))
        val optArea = underlayAccess.read(id).checkedGet()
        if (!optArea.isPresent) {
            return emptyList()
        }
        return optArea.get()?.`interface`.orEmpty()
                .map {
                    InterfaceKey(String(it.name.value))
                }
    }
}
