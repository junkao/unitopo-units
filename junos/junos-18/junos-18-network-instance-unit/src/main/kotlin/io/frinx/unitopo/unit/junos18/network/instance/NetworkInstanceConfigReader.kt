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

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.network.instance.vrf.VrfConfigReader
import io.frinx.unitopo.unit.network.instance.common.def.DefaultConfigReader
import io.frinx.unitopo.unit.network.instance.NetworkInstanceConfigReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder

class NetworkInstanceConfigReader(access: UnderlayAccess) :
    NetworkInstanceConfigReader(object : ArrayList<ReaderCustomizer<Config, ConfigBuilder>>() {
        init {
            add(DefaultConfigReader())
            add(VrfConfigReader(access))
        }
    })