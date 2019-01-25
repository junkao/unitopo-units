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
package io.frinx.unitopo.unit.xr6.lldp

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.OperListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ethernet.lldp.oper.rev151109.Lldp
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp._interface.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.InterfaceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ethernet.lldp.oper.rev151109.lldp.nodes.node.interfaces.Interface as UnderlayLldpInterface

class InterfaceReader(private val underlayAccess: UnderlayAccess) :
    OperListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

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