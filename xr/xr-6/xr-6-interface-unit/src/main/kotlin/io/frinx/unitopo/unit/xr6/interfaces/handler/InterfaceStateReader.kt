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

package io.frinx.unitopo.unit.xr6.interfaces.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.Util
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730.ImStateEnum
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceCommonState
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.StateBuilder
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timeticks
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MDSalReadFailed
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730._interface.table.interfaces.Interface as OperInterface

class InterfaceStateReader(private val underlayAccess: UnderlayAccess) : OperReaderCustomizer<State, StateBuilder> {

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<State>,
        stateBuilder: StateBuilder,
        readContext: ReadContext
    ) {
        try {
            // Using InterfaceConfiguration and also InterfaceProperties to collect all necessary information
            val name = instanceIdentifier.firstKeyOf(Interface::class.java).name
            InterfaceReader.readInterfaceCfg(underlayAccess, name, { stateBuilder.fromUnderlay(it) })
            InterfaceReader.readInterfaceProps(underlayAccess, name, { stateBuilder.fromUnderlayProps(it) })
        } catch (e: MDSalReadFailed) {
            throw ReadFailedException(instanceIdentifier, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, state: State) {
        (builder as InterfaceBuilder).state = state
    }
}

fun StateBuilder.fromUnderlay(underlay: InterfaceConfiguration) {
    name = underlay.interfaceName.value
    description = underlay.description
    isEnabled = underlay.isShutdown == null
}

fun StateBuilder.fromUnderlayProps(underlay: OperInterface) {
    type = Util.parseIfcType(underlay.interfaceName.value)
    mtu = underlay.mtu.toInt()
    ifindex = 0
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