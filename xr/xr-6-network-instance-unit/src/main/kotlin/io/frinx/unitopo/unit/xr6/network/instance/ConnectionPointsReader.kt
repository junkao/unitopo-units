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

package io.frinx.unitopo.unit.xr6.network.instance

import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.translate.unit.commons.handler.spi.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.network.instance.l2p2p.cp.L2P2PConnectionPointsReader
import io.frinx.unitopo.unit.xr6.network.instance.l2vsi.cp.L2VSIConnectionPointsReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPoints
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPointsBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.ArrayList

class ConnectionPointsReader(cli: UnderlayAccess) : ConfigReaderCustomizer<ConnectionPoints, ConnectionPointsBuilder>,
        CompositeReader<ConnectionPoints, ConnectionPointsBuilder>(
            object : ArrayList<ReaderCustomizer<ConnectionPoints, ConnectionPointsBuilder>>() {
    init {
        add(L2P2PConnectionPointsReader(cli))
        add(L2VSIConnectionPointsReader(cli))
    }
}), ReaderCustomizer<ConnectionPoints, ConnectionPointsBuilder> {

    override fun getBuilder(id: InstanceIdentifier<ConnectionPoints>): ConnectionPointsBuilder {
        return ConnectionPointsBuilder()
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: ConnectionPoints) {
        (parentBuilder as NetworkInstanceBuilder).connectionPoints = readValue
    }
}