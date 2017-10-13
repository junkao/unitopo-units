/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.vrf

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.State
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.StateBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class ProtocolStateReader(private val underlayAccess: UnderlayAccess) : ReaderCustomizer<State, StateBuilder> {

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<State>): StateBuilder {
        return StateBuilder()
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(instanceIdentifier: InstanceIdentifier<State>, configBuilder: StateBuilder, readContext: ReadContext) {
        if (underlayAccess.currentOperationType == LogicalDatastoreType.CONFIGURATION) {
            return
        }
        
        val protocolKey = instanceIdentifier.firstKeyOf(Protocol::class.java)
        configBuilder.`identifier` = protocolKey.identifier
        configBuilder.`name` = protocolKey.name
    }

    override fun merge(builder: Builder<out DataObject>, state: State) {
        (builder as ProtocolBuilder).`state` = state
    }
}