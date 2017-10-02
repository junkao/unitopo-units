/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.interfaces

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730.ImStateEnum
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730.InterfaceProperties
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730._interface.properties.DataNodes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730._interface.properties.DataNodesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.InterfaceCommonState
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.State
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.StateBuilder
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timeticks
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MDSalReadFailed
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730._interface.table.interfaces.Interface as OperInterface

class InterfaceStateReader(private val underlayAccess: UnderlayAccess) : ReaderCustomizer<State, StateBuilder> {

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<State>): StateBuilder = StateBuilder()

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(instanceIdentifier: InstanceIdentifier<State>,
                                       stateBuilder: StateBuilder,
                                       readContext: ReadContext) {
        // FIXME move this check into interface with default method
        if (underlayAccess.currentOperationType == LogicalDatastoreType.CONFIGURATION) return

        try {
            val name = instanceIdentifier.firstKeyOf(Interface::class.java).name

            // Aggregate system wide interface properties and map them by ifc-name
            val ifcPropertiesMapped = underlayAccess.read(DATA_NODES_ID)
                    .checkedGet()
                    .or(EMPTY_DATA_NODES)
                    .dataNode
                    ?.flatMap { it.systemView.interfaces.`interface`.orEmpty() }
                    ?.map { Pair(it.interfaceName, it) }
                    ?.toMap()
                    .orEmpty()

            // Getting all configurations and filtering here due to:
            //  - interfaces in underlay are keyed by: name + state compared to only ifc name in openconfig models
            //  - the read is performed in multiple places and with caching its for free
            underlayAccess.read(InterfaceReader.IFC_CFGS)
                    .checkedGet()
                    .orNull()
                    ?.let {
                        it.`interfaceConfiguration`
                                ?.filter { it.interfaceName.value == name }
                                ?.first()
                                ?.let { stateBuilder.fromUnderlay(it, ifcPropertiesMapped[it.interfaceName]) }
                    }
        } catch (e: MDSalReadFailed) {
            throw ReadFailedException(instanceIdentifier, e)
        }

    }

    override fun merge(builder: Builder<out DataObject>, state: State) {
        (builder as InterfaceBuilder).state = state
    }

    companion object {
        val DATA_NODES_ID = InstanceIdentifier.create(InterfaceProperties::class.java).child(DataNodes::class.java)!!
        val EMPTY_DATA_NODES = DataNodesBuilder().build()!!
    }
}

fun StateBuilder.fromUnderlay(underlay: InterfaceConfiguration, underlayOper: OperInterface?) {
    type = parseIfcType(underlay.interfaceName.value)
    name = underlay.interfaceName.value
    description = underlay.description
    isEnabled =  underlay.isShutdown == null

    underlayOper?.let {
        mtu = underlayOper.mtu.toInt()
        ifindex = 0
        lastChange = Timeticks(0)
        adminStatus = when {
            underlayOper.actualState == ImStateEnum.ImStateUp -> InterfaceCommonState.AdminStatus.UP
            underlayOper.actualState == ImStateEnum.ImStateOperational -> InterfaceCommonState.AdminStatus.UP
            else -> InterfaceCommonState.AdminStatus.DOWN
        }
        operStatus = when {
            underlayOper.actualState == ImStateEnum.ImStateUp -> InterfaceCommonState.OperStatus.UP
            underlayOper.actualState == ImStateEnum.ImStateOperational -> InterfaceCommonState.OperStatus.UP
            underlayOper.actualState == ImStateEnum.ImStateDown -> InterfaceCommonState.OperStatus.DOWN
            underlayOper.actualState == ImStateEnum.ImStateAdminDown -> InterfaceCommonState.OperStatus.DOWN
            underlayOper.actualState == ImStateEnum.ImStateNotOperational -> InterfaceCommonState.OperStatus.DOWN
            underlayOper.actualState == ImStateEnum.ImStateNotReady -> InterfaceCommonState.OperStatus.DORMANT
            else -> InterfaceCommonState.OperStatus.UNKNOWN
        }
    }
}