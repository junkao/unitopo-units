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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ethernet.lldp.oper.rev151109.Lldp
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp._interface.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.InterfaceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ethernet.lldp.oper.rev151109.lldp.nodes.node.interfaces.Interface as UnderlayLldpInterface

class InterfaceReader(private val underlayAccess: UnderlayAccess) : ListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    override fun getAllIds(id: InstanceIdentifier<Interface>, context: ReadContext) = getInterfaceIds(underlayAccess)

    override fun readCurrentAttributes(id: InstanceIdentifier<Interface>, builder: InterfaceBuilder, ctx: ReadContext) {
        // TODO check reading existing interface
        builder.name = id.firstKeyOf(Interface::class.java).name
    }

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<Interface>) {
        (builder as InterfacesBuilder).`interface` = readData
    }

    override fun getBuilder(id: InstanceIdentifier<Interface>) = InterfaceBuilder()

    companion object {
        val LLDP_OPER = InstanceIdentifier.create(Lldp::class.java)!!

        fun getInterfaceIds(underlayAccess: UnderlayAccess) = parseInterfaceIds(getInterfaces(underlayAccess))

        fun parseInterfaceIds(list: List<UnderlayLldpInterface>) = list
            .map { it.interfaceName.value }
            .map { InterfaceKey(it) }

        private fun getUnderlayLldp(underlayAccess: UnderlayAccess) = underlayAccess
            .read(LLDP_OPER, LogicalDatastoreType.OPERATIONAL)
            .checkedGet()
            .orNull()

        private fun getInterfaces(underlayAccess: UnderlayAccess) = parseInterfaces(getUnderlayLldp(underlayAccess))

        fun parseInterfaces(lldp: Lldp?) = lldp?.nodes?.node.orEmpty()
            .flatMap { it.interfaces?.`interface`.orEmpty() }

        fun readInterfaceNeighbors(underlayAccess: UnderlayAccess, name: String) =
            parseInterfaceNeighbors(name, getUnderlayLldp(underlayAccess))

        fun parseInterfaceNeighbors(name: String, lldp: Lldp?) = lldp?.nodes?.node.orEmpty()
            .flatMap { it.neighbors?.summaries?.summary.orEmpty() }
            .filter { it.interfaceName.value == name }
    }
}