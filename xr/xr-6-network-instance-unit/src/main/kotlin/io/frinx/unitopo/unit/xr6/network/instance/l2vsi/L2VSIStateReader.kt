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

package io.frinx.unitopo.unit.xr6.network.instance.l2vsi

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.translate.unit.commons.registry.common.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.StateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L2VSI
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class L2VSIStateReader(private val cli: UnderlayAccess) :
    OperReaderCustomizer<State, StateBuilder>, CompositeReader.Child<State, StateBuilder> {

    override fun getBuilder(p0: InstanceIdentifier<State>): StateBuilder {
        // NOOP
        throw UnsupportedOperationException("Should not be invoked")
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<State>,
        stateBuilder: StateBuilder,
        readContext: ReadContext
    ) {
        if (isVrf(instanceIdentifier)) {
            stateBuilder.name = instanceIdentifier.firstKeyOf<NetworkInstance,
                NetworkInstanceKey>(NetworkInstance::class.java).name
            stateBuilder.type = L2VSI::class.java

            // TODO set other attributes i.e. description
        }
    }

    @Throws(ReadFailedException::class)
    private fun isVrf(id: InstanceIdentifier<State>): Boolean {
        return L2VSIReader.getAllIds(cli).contains(id.firstKeyOf<NetworkInstance,
            NetworkInstanceKey>(NetworkInstance::class.java))
    }
}