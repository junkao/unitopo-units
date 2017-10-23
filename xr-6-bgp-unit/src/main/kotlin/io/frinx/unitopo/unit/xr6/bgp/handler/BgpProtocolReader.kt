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
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.common.BgpReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.Bgp
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedEx
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class BgpProtocolReader(private val access: UnderlayAccess) : ListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder>,
        BgpReader<Protocol, ProtocolBuilder> {

    @Throws(ReadFailedException::class)
    override fun getAllIds(id: IID<Protocol>, context: ReadContext): List<ProtocolKey> {
        try {
            return access.read(UNDERLAY_BGP)
                    .checkedGet()
                    .orNull()
                    ?.let {
                        it.instance
                                .orEmpty()
                                // FIXME filter only per VRF
                                .map { ProtocolKey(TYPE, it.instanceName.value) }
                    }.orEmpty()
        } catch (e: MdSalReadFailedEx) {
            throw ReadFailedException(id, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, readData: List<Protocol>) {
        // NOOP
        // TODO Protocol/typed reader
    }

    override fun getBuilder(id: IID<Protocol>) = ProtocolBuilder()

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributesForType(id: IID<Protocol>, builder: ProtocolBuilder, ctx: ReadContext) {
        val key = id.firstKeyOf(Protocol::class.java)
        builder.name = key.name
        builder.identifier = key.identifier
    }

    companion object {
        val UNDERLAY_BGP = IID.create(Bgp::class.java)!!
        val TYPE: Class<BGP> = BGP::class.java
    }
}
