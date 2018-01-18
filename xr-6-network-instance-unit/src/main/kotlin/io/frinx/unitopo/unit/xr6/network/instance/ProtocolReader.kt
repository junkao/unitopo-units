/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import io.frinx.unitopo.unit.xr6.lr.handler.StaticProtocolReader
import io.frinx.unitopo.unit.network.instance.common.L3VrfListReader
import io.frinx.unitopo.unit.network.instance.protocol.ProtocolReaderComposite
import io.frinx.unitopo.unit.xr6.ospf.handler.OspfProtocolReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class ProtocolReader(cli: UnderlayAccess) : L3VrfListReader.L3VrfConfigListReader<Protocol, ProtocolKey, ProtocolBuilder> {

    private val delegate: XrProtocolReaderComposite

    init {
        // Wrapping the composite reader into a typed reader to ensure network instance type first
        delegate = XrProtocolReaderComposite(cli)
    }

    @Throws(ReadFailedException::class)
    override fun getAllIdsForType(instanceIdentifier: InstanceIdentifier<Protocol>, readContext: ReadContext): List<ProtocolKey> {
        return delegate.getAllIds(instanceIdentifier, readContext)
    }

    override fun merge(builder: Builder<out DataObject>, readData: List<Protocol>) {
        delegate.merge(builder, readData)
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributesForType(instanceIdentifier: InstanceIdentifier<Protocol>, protocolBuilder: ProtocolBuilder, readContext: ReadContext) {
        delegate.readCurrentAttributes(instanceIdentifier, protocolBuilder, readContext)
    }

    override fun getBuilder(id: InstanceIdentifier<Protocol>): ProtocolBuilder {
        return delegate.getBuilder(id)
    }

    class XrProtocolReaderComposite(cli: UnderlayAccess) : ProtocolReaderComposite(object : ArrayList<ListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder>>() {
        init {
            add(OspfProtocolReader(cli))
            add(BgpProtocolReader(cli))
            add(StaticProtocolReader())
        }
    })
}
