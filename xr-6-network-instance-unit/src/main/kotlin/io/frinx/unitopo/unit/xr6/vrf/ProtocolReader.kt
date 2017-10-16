/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.vrf

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import io.frinx.unitopo.unit.xr6.lr.handler.StaticProtocolReader
import io.frinx.unitopo.unit.xr6.ospf.handler.OspfProtocolReader
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ProtocolsBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.*


class ProtocolReader(access: UnderlayAccess) : ListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder> {

    private val specificReaders: List<ListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder>>

    init {
        specificReaders = object : ArrayList<ListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder>>() {
            init {
                add(BgpProtocolReader(access))
                add(OspfProtocolReader(access))
                add(StaticProtocolReader(access))
            }
        }
    }

    @Throws(ReadFailedException::class)
    override fun getAllIds(id: InstanceIdentifier<Protocol>, context: ReadContext): List<ProtocolKey> {
        val allIds = ArrayList<ProtocolKey>()
        for (specificReader in specificReaders) {
            allIds.addAll(specificReader.getAllIds(id, context))
        }
        return allIds
    }

    override fun merge(builder: Builder<out DataObject>, list: List<Protocol>) {
        (builder as ProtocolsBuilder).`protocol` = list
    }

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<Protocol>): ProtocolBuilder {
        return ProtocolBuilder()
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(id: InstanceIdentifier<Protocol>, builder: ProtocolBuilder, ctx: ReadContext) {
        // Invoking all specific readers here, each reader is responsible to check whether it is its type of
        // protocol and if so, set the values
        for (specificReader in specificReaders) {
            specificReader.readCurrentAttributes(id, builder, ctx)
        }
    }
}
