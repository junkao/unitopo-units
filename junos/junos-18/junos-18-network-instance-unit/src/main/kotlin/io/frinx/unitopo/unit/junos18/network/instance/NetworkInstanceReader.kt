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

package io.frinx.unitopo.unit.junos18.network.instance

import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.unitopo.handlers.network.instance.def.DefaultReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.network.instance.vrf.VrfReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey

class NetworkInstanceReader(access: UnderlayAccess) :
    CompositeListReader<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder>(
        listOf(
            DefaultReader(),
            VrfReader(access)
        )
    ), ConfigListReaderCustomizer<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder>