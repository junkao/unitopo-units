/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lldp

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces._interface.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces._interface.StateBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceStateReader(private val underlayAccess: UnderlayAccess) : OperReaderCustomizer<State, StateBuilder> {

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: State) {
        (parentBuilder as InterfaceBuilder).state = readValue
    }

    override fun readCurrentAttributes(id: InstanceIdentifier<State>, builder: StateBuilder, ctx: ReadContext) {
        builder.name = id.firstKeyOf(Interface::class.java).name
        builder.isEnabled = true
    }

    override fun getBuilder(id: InstanceIdentifier<State>) = StateBuilder()
}