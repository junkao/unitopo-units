/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lldp

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ethernet.lldp.oper.rev151109.lldp.nodes.node.neighbors.summaries.Summary
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.NeighborsBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.neighbors.Neighbor
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.neighbors.NeighborBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.neighbors.NeighborKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NeighborReader(private val underlayAccess: UnderlayAccess) : ListReaderCustomizer<Neighbor, NeighborKey, NeighborBuilder> {

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<Neighbor>) {
        (builder as NeighborsBuilder).neighbor = readData
    }

    override fun readCurrentAttributes(id: InstanceIdentifier<Neighbor>, builder: NeighborBuilder, ctx: ReadContext) {
        if (underlayAccess.currentOperationType == LogicalDatastoreType.OPERATIONAL) {
            builder.id = id.firstKeyOf(Neighbor::class.java).id
        }
    }

    override fun getAllIds(id: InstanceIdentifier<Neighbor>, context: ReadContext) : List<NeighborKey> {
        return if (underlayAccess.currentOperationType == LogicalDatastoreType.OPERATIONAL) {
            parseDeviceIds(InterfaceReader.readInterfaceNeighbors(underlayAccess, id.firstKeyOf(Interface::class.java).name))
        } else {
            emptyList()
        }
    }

    override fun getBuilder(id: InstanceIdentifier<Neighbor>) = NeighborBuilder()

    companion object {
        fun parseDeviceIds(summaries: List<Summary>) = summaries
            .map { it.deviceId }
            .map { NeighborKey(it) }
    }
}
