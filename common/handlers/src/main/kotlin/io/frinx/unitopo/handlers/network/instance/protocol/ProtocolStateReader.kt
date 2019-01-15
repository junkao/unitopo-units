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
package io.frinx.unitopo.handlers.network.instance.protocol

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.unitopo.handlers.l3vrf.L3VrfReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.StateBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class ProtocolStateReader : L3VrfReader.L3VrfOperReader<State, StateBuilder> {

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<State>): StateBuilder {
        return StateBuilder()
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributesForType(
        instanceIdentifier: InstanceIdentifier<State>,
        StateBuilder: StateBuilder,
        readContext: ReadContext
    ) {
        val protocolKey = instanceIdentifier.firstKeyOf(Protocol::class.java)
        StateBuilder.identifier = protocolKey.identifier
        StateBuilder.name = protocolKey.name
    }

    override fun merge(builder: Builder<out DataObject>, State: State) {
        (builder as ProtocolBuilder).state = State
    }
}