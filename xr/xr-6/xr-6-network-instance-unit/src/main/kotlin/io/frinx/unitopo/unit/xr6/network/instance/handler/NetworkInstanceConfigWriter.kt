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

package io.frinx.unitopo.unit.xr6.network.instance.handler

import io.frinx.translate.unit.commons.handler.spi.CompositeWriter
import io.frinx.unitopo.ni.base.handler.vrf.def.DefaultConfigWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.network.instance.handler.l2p2p.L2P2PConfigWriter
import io.frinx.unitopo.unit.xr6.network.instance.handler.vrf.L3VrfConfigWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config

class NetworkInstanceConfigWriter(access: UnderlayAccess) : CompositeWriter<Config>(
    listOf(
        L3VrfConfigWriter(access),
        DefaultConfigWriter(),
        L2P2PConfigWriter(access)
    )
)