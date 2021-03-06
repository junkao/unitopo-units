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

import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.translate.unit.commons.handler.spi.CompositeReader
import io.frinx.unitopo.ni.base.handler.vrf.def.DefaultStateReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.network.instance.handler.l2p2p.L2P2PStateReader
import io.frinx.unitopo.unit.xr6.network.instance.handler.l2vsi.L2VSIStateReader
import io.frinx.unitopo.unit.xr6.network.instance.handler.vrf.L3VrfStateReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.StateBuilder

class NetworkInstanceStateReader(access: UnderlayAccess) : CompositeReader<State, StateBuilder>(
    listOf(
        L3VrfStateReader(access),
        DefaultStateReader(),
        L2P2PStateReader(access),
        L2VSIStateReader(access)
    )
), OperReaderCustomizer<State, StateBuilder>