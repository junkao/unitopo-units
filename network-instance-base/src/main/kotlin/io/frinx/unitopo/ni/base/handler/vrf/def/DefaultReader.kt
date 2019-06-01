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

package io.frinx.unitopo.ni.base.handler.vrf.def

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.builder.BasicCheck
import io.fd.honeycomb.translate.spi.builder.Check
import io.frinx.openconfig.network.instance.NetworInstance.DEFAULT_NETWORK
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class DefaultReader : CompositeListReader.Child<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder> {

    override fun getCheck(): Check {
        return BasicCheck.emptyCheck()
    }

    override fun getAllIds(
        instanceIdentifier: InstanceIdentifier<NetworkInstance>,
        readContext: ReadContext
    ): List<NetworkInstanceKey> = listOf(DEFAULT_NETWORK)

    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<NetworkInstance>,
        networkInstanceBuilder: NetworkInstanceBuilder,
        readContext: ReadContext
    ) {
        val name = instanceIdentifier.firstKeyOf(NetworkInstance::class.java).name
        networkInstanceBuilder.name = name
    }
}