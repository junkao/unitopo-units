/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.l2p2p

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.cli.registry.common.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.StateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L2P2P
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class L2P2PStateReader(private val underlayAccess: UnderlayAccess) : OperReaderCustomizer<State, StateBuilder>, CompositeReader.Child<State, StateBuilder> {

    override fun readCurrentAttributes(instanceIdentifier: InstanceIdentifier<State>,
                                       configBuilder: StateBuilder,
                                       readContext: ReadContext) {
        if (isP2P(instanceIdentifier, readContext)) {
            configBuilder.name = instanceIdentifier.firstKeyOf<NetworkInstance, NetworkInstanceKey>(NetworkInstance::class.java).name
            configBuilder.type = L2P2P::class.java

            // TODO set other attributes i.e. description
        }
    }

    private fun isP2P(id: InstanceIdentifier<State>, readContext: ReadContext): Boolean {
        // FIXME
        return false
    }
}
