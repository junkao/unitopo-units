/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.l2p2p.cp

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.read.Reader
import io.frinx.cli.registry.common.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.network.instance.common.L2p2pReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPoints
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPointsBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class L2P2PConnectionPointsReader(private val underlayAccess: UnderlayAccess) : L2p2pReader<ConnectionPoints, ConnectionPointsBuilder>,
        CompositeReader.Child<ConnectionPoints, ConnectionPointsBuilder> {

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributesForType(id: InstanceIdentifier<ConnectionPoints>,
                                              builder: ConnectionPointsBuilder,
                                              ctx: ReadContext) {
        // FIXME
    }

    private fun isOper(ctx: ReadContext): Boolean {
        val flag = ctx.modificationCache.get(Reader.DS_TYPE_FLAG)
        return flag != null && flag === LogicalDatastoreType.OPERATIONAL
    }


}
