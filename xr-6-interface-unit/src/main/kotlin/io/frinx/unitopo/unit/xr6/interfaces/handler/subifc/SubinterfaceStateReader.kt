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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730.ImStateEnum
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceCommonState
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.StateBuilder
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timeticks
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730._interface.table.interfaces.Interface as OperInterface

class SubinterfaceStateReader(private val underlayAccess: UnderlayAccess) : OperReaderCustomizer<State, StateBuilder> {

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: State) {
        (parentBuilder as SubinterfaceBuilder).state = readValue
    }

    override fun readCurrentAttributes(id: InstanceIdentifier<State>, builder: StateBuilder, ctx: ReadContext) {
        // TODO Make a util function that will parse out subifc name out of
        // instanceId
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val subifcIndex = id.firstKeyOf(Subinterface::class.java).index

        builder.index = subifcIndex

        val subifcName = getSubIfcName(ifcName, subifcIndex)

        InterfaceReader.readInterfaceCfg(underlayAccess, subifcName, { builder.fromUnderlay(it) })
        InterfaceReader.readInterfaceProps(underlayAccess, subifcName, { builder.fromUnderlayProps(it) })
    }

    override fun getBuilder(id: InstanceIdentifier<State>): StateBuilder = StateBuilder()
}

private const val SUBINTERFACE_SEPARATOR = "."

fun getSubIfcName(ifcName: String, subifcIdx: Long) = ifcName + SUBINTERFACE_SEPARATOR + subifcIdx

private fun StateBuilder.fromUnderlay(underlay: InterfaceConfiguration) {
    name = underlay.interfaceName.value
    description = underlay.description
    isEnabled = underlay.isShutdown == null
}

private fun StateBuilder.fromUnderlayProps(underlay: OperInterface) {
    lastChange = Timeticks(0)
    adminStatus = when {
        underlay.actualState == ImStateEnum.ImStateUp -> InterfaceCommonState.AdminStatus.UP
        underlay.actualState == ImStateEnum.ImStateOperational -> InterfaceCommonState.AdminStatus.UP
        else -> InterfaceCommonState.AdminStatus.DOWN
    }
    operStatus = when {
        underlay.actualState == ImStateEnum.ImStateUp -> InterfaceCommonState.OperStatus.UP
        underlay.actualState == ImStateEnum.ImStateOperational -> InterfaceCommonState.OperStatus.UP
        underlay.actualState == ImStateEnum.ImStateDown -> InterfaceCommonState.OperStatus.DOWN
        underlay.actualState == ImStateEnum.ImStateAdminDown -> InterfaceCommonState.OperStatus.DOWN
        underlay.actualState == ImStateEnum.ImStateNotOperational -> InterfaceCommonState.OperStatus.DOWN
        underlay.actualState == ImStateEnum.ImStateNotReady -> InterfaceCommonState.OperStatus.DORMANT
        else -> InterfaceCommonState.OperStatus.UNKNOWN
    }
}