/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lldp

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ethernet.lldp.oper.rev151109.lldp.nodes.node.neighbors.summaries.Summary
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.neighbors.Neighbor
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.neighbors.NeighborBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.neighbors.neighbor.State
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.neighbors.neighbor.StateBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NeighborStateReader(private val underlayAccess: UnderlayAccess) : ReaderCustomizer<State, StateBuilder> {

    override fun getBuilder(id: InstanceIdentifier<State>) = StateBuilder()

    override fun merge(builder: Builder<out DataObject>, readValue: State) {
        (builder as NeighborBuilder).state = readValue
    }

    override fun readCurrentAttributes(id: InstanceIdentifier<State>, builder: StateBuilder, ctx: ReadContext) {
        if (underlayAccess.currentOperationType == LogicalDatastoreType.OPERATIONAL) {
            InterfaceReader.readInterfaceNeighbors(underlayAccess, id.firstKeyOf(Interface::class.java).name)
                    .find { it.deviceId == id.firstKeyOf(Neighbor::class.java).id }
                    ?.let { builder.fromUnderlay(it) }
        }
    }
}

private fun StateBuilder.fromUnderlay(it: Summary) {
    // Getting first element from list, the list always contains only 1 value
    portId = it.lldpNeighbor.orEmpty()[0].portIdDetail
}
