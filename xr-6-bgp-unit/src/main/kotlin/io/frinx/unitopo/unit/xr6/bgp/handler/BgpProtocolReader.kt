/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.cli.registry.common.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.network.instance.protocol.bgp.common.BgpReader
import io.frinx.unitopo.unit.xr6.bgp.IID
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.Bgp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey

class BgpProtocolReader(private val access: UnderlayAccess) :
        BgpReader.BgpConfigReader<Protocol, ProtocolBuilder>,
        CompositeListReader.Child<Protocol, ProtocolKey, ProtocolBuilder> {

    override fun getBuilder(p0: org.opendaylight.yangtools.yang.binding.InstanceIdentifier<Protocol>): ProtocolBuilder {
        // NOOP
        throw UnsupportedOperationException("Should not be invoked")
    }

    @Throws(ReadFailedException::class)
    override fun getAllIds(id: IID<Protocol>, context: ReadContext): List<ProtocolKey> {
        val data = access.read(BgpProtocolReader.UNDERLAY_BGP, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()
        return parseIds(data)
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributesForType(id: IID<Protocol>, builder: ProtocolBuilder, ctx: ReadContext) {
        val key = id.firstKeyOf(Protocol::class.java)
        builder.name = key.name
        builder.identifier = key.identifier
    }

    companion object {
        val UNDERLAY_BGP = IID.create(Bgp::class.java)!!

        fun getFirst4ByteAs(underlayInstance: Instance?): FourByteAs? =
                underlayInstance?.instanceAs.orEmpty().firstOrNull()
                        ?.fourByteAs.orEmpty().firstOrNull()

        fun parseIds(data: Bgp?): List<ProtocolKey> {
            return data
                    ?.let {
                        it.instance
                                .orEmpty()
                                .map { ProtocolKey(BgpReader.TYPE, it.instanceName.value) }
                    }.orEmpty()
        }

    }
}
