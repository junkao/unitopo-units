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

import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.translate.unit.commons.handler.spi.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.network.instance.handler.l2p2p.cp.L2P2PConnectionPointsReader
import io.frinx.unitopo.unit.xr6.network.instance.handler.l2vsi.cp.L2VSIConnectionPointsReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPoints
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPointsBuilder

class ConnectionPointsReader(cli: UnderlayAccess) : CompositeReader<ConnectionPoints, ConnectionPointsBuilder> (
        listOf(
            L2P2PConnectionPointsReader(cli),
            L2VSIConnectionPointsReader(cli)
        )
), ConfigReaderCustomizer<ConnectionPoints, ConnectionPointsBuilder>