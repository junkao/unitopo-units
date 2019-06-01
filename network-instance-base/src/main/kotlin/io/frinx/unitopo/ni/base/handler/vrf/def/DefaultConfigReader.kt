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
import io.frinx.translate.unit.commons.handler.spi.CompositeReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.DEFAULTINSTANCE
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class DefaultConfigReader : CompositeReader.Child<Config, ConfigBuilder> {

    override fun getCheck(): Check {
        return BasicCheck.emptyCheck()
    }

    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        if (isDefault(instanceIdentifier)) {
            configBuilder.name = instanceIdentifier
                .firstKeyOf<NetworkInstance, NetworkInstanceKey>(NetworkInstance::class.java).name
            configBuilder.type = DEFAULTINSTANCE::class.java
        }
    }

    companion object {

        internal fun isDefault(instanceIdentifier: InstanceIdentifier<*>): Boolean {
            return instanceIdentifier
                .firstKeyOf<NetworkInstance, NetworkInstanceKey>(NetworkInstance::class.java) == DEFAULT_NETWORK
        }
    }
}