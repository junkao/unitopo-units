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
package io.frinx.unitopo.unit.junos17.network.instance

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.unitopo.handlers.l3vrf.L3VrfListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpProtocolReader
import io.frinx.unitopo.unit.junos.ospf.handler.OspfProtocolReader
import io.frinx.unitopo.handlers.network.instance.protocol.ProtocolReaderComposite
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.ArrayList

class ProtocolReader(access: UnderlayAccess) :
    L3VrfListReader.L3VrfConfigListReader<Protocol, ProtocolKey, ProtocolBuilder> {

    private val delegate: JunosProtocolReaderComposite

    init {
        // Wrapping the composite reader into a typed reader to ensure network instance type first
        delegate = JunosProtocolReaderComposite(access)
    }

    @Throws(ReadFailedException::class)
    override fun getAllIdsForType(instanceIdentifier: InstanceIdentifier<Protocol>, readContext: ReadContext):
        List<ProtocolKey> {
        return delegate.getAllIds(instanceIdentifier, readContext)
    }

    override fun merge(builder: Builder<out DataObject>, readData: List<Protocol>) {
        delegate.merge(builder, readData)
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributesForType(
        instanceIdentifier: InstanceIdentifier<Protocol>,
        protocolBuilder: ProtocolBuilder,
        readContext: ReadContext
    ) {
        delegate.readCurrentAttributes(instanceIdentifier, protocolBuilder, readContext)
    }

    override fun getBuilder(id: InstanceIdentifier<Protocol>): ProtocolBuilder {
        return delegate.getBuilder(id)
    }

    class JunosProtocolReaderComposite(access: UnderlayAccess) :
        ProtocolReaderComposite(object : ArrayList<ListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder>>() {
        init {
            add(BgpProtocolReader(access))
            add(OspfProtocolReader(access))
        }
    })
}