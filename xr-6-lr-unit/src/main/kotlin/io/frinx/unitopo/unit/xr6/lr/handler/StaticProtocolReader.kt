/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lr.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.cli.registry.common.CompositeListReader
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.unit.xr6.lr.common.LrReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class StaticProtocolReader :
        LrReader.LrConfigReader<Protocol, ProtocolBuilder>,
        CompositeListReader.Child<Protocol, ProtocolKey, ProtocolBuilder> {

    override fun getBuilder(p0: org.opendaylight.yangtools.yang.binding.InstanceIdentifier<Protocol>): ProtocolBuilder {
        // NOOP
        throw UnsupportedOperationException("Should not be invoked")
    }

    @Throws(ReadFailedException::class)
    override fun getAllIds(id: IID<Protocol>, context: ReadContext): List<ProtocolKey> = listOf(ProtocolKey(LrReader.TYPE, NetworInstance.DEFAULT_NETWORK_NAME))

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributesForType(id: IID<Protocol>, builder: ProtocolBuilder, ctx: ReadContext) {
        val key = id.firstKeyOf(Protocol::class.java)
        builder.name = key.name
        builder.identifier = key.identifier
    }
}
