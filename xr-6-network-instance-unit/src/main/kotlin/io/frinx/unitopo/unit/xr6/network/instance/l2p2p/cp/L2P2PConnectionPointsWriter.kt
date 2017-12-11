/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.l2p2p.cp

import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.network.instance.common.L2p2pWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPoints
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class L2P2PConnectionPointsWriter(private val underlayAccess: UnderlayAccess) : L2p2pWriter<ConnectionPoints> {

    @Throws(WriteFailedException::class)
    override fun writeCurrentAttributesForType(id: InstanceIdentifier<ConnectionPoints>,
                                               dataAfter: ConnectionPoints,
                                               writeContext: WriteContext) {
        // FIXME
    }

    @Throws(WriteFailedException.DeleteFailedException::class)
    override fun deleteCurrentAttributesForType(id: InstanceIdentifier<ConnectionPoints>,
                                                dataBefore: ConnectionPoints,
                                                writeContext: WriteContext) {
        // FIXME
    }

    @Throws(WriteFailedException::class)
    override fun updateCurrentAttributesForType(id: InstanceIdentifier<ConnectionPoints>,
                                                dataBefore: ConnectionPoints,
                                                dataAfter: ConnectionPoints, writeContext: WriteContext) {
        deleteCurrentAttributes(id, dataBefore, writeContext)
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

}
