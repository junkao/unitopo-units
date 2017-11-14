/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class SubinterfaceReader(private val underlayAccess: UnderlayAccess) : ConfigListReaderCustomizer<Subinterface, SubinterfaceKey, SubinterfaceBuilder> {

    override fun getAllIds(id: InstanceIdentifier<Subinterface>, context: ReadContext): MutableList<SubinterfaceKey> {
        val ifcName = id.firstKeyOf(Interface::class.java).name

        return if (InterfaceReader.interfaceExists(underlayAccess, id)) {

            // TODO We are misusing the InterfaceReader.getInterfaceIds
            // function. We should create own getSubinterfaceIds function
            // so we can write UT and filter subinterfaces already out of
            // underlay ifc list.
            val subIfcKeys = InterfaceReader.getInterfaceIds(underlayAccess)
                    .filter { InterfaceReader.isSubinterface(it.name) }
                    .filter { it.name.startsWith(ifcName)}
                    .map { InterfaceReader.getSubinterfaceKey(it.name) }

            // Add the 0 subinterface for IP addresses if there is such interface
            subIfcKeys.plus(SubinterfaceKey(0L)).toMutableList()

        } else {
            emptyList<SubinterfaceKey>().toMutableList()
        }
    }

    override fun readCurrentAttributes(id: InstanceIdentifier<Subinterface>, builder: SubinterfaceBuilder, ctx: ReadContext) {
        builder.index = id.firstKeyOf(Subinterface::class.java).index
    }

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<Subinterface>) {
        (builder as SubinterfacesBuilder).subinterface = readData
    }

    override fun getBuilder(p0: InstanceIdentifier<Subinterface>): SubinterfaceBuilder = SubinterfaceBuilder()
}