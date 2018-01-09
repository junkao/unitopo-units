/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.vrf

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.cli.registry.common.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.StateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L3VRF
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class VrfStateReader(private val cli: UnderlayAccess) : OperReaderCustomizer<State, StateBuilder>,
        CompositeReader.Child<State, StateBuilder> {

    override fun getBuilder(p0: InstanceIdentifier<State>): StateBuilder {
        // NOOP
        throw UnsupportedOperationException("Should not be invoked")
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(instanceIdentifier: InstanceIdentifier<State>,
                                       stateBuilder: StateBuilder,
                                       readContext: ReadContext) {
        if (isVrf(instanceIdentifier)) {
            stateBuilder.name = instanceIdentifier.firstKeyOf<NetworkInstance, NetworkInstanceKey>(NetworkInstance::class.java).name
            stateBuilder.type = L3VRF::class.java

            // TODO set other attributes i.e. description
        }
    }

    @Throws(ReadFailedException::class)
    private fun isVrf(id: InstanceIdentifier<State>): Boolean {
        return VrfReader.getAllIds(cli).contains(id.firstKeyOf<NetworkInstance, NetworkInstanceKey>(NetworkInstance::class.java))
    }
}
