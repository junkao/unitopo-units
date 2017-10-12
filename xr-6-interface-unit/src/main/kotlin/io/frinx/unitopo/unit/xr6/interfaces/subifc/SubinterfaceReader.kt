/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.frinx.unitopo.unit.xr6.interfaces.subifc

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.InterfaceReader
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class SubinterfaceReader(private val underlayAccess: UnderlayAccess) : ListReaderCustomizer<Subinterface, SubinterfaceKey, SubinterfaceBuilder> {

    override fun getAllIds(id: InstanceIdentifier<Subinterface>, context: ReadContext): MutableList<SubinterfaceKey> {
        return if (InterfaceReader.interfaceExists(underlayAccess, id)) {
            // Add the 0 subinterface for IP addresses if there is such interface
            listOf(0L)
                    .map { SubinterfaceKey(it) }
                    .toMutableList()
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