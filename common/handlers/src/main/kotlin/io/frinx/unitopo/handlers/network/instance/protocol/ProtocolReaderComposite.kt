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

import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ProtocolsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class ProtocolReaderComposite(readers: ArrayList<ListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder>>) :
    CompositeListReader<Protocol, ProtocolKey, ProtocolBuilder>(readers),
    ListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder> {

    override fun merge(builder: Builder<out DataObject>, list: List<Protocol>) {
        (builder as ProtocolsBuilder).protocol = list
    }

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<Protocol>): ProtocolBuilder {
        return ProtocolBuilder()
    }
}