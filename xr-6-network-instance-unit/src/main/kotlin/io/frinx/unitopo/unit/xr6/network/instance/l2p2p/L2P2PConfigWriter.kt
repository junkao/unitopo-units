/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.l2p2p

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class L2P2PConfigWriter(private val cli: UnderlayAccess) : WriterCustomizer<Config> {

    @Throws(WriteFailedException.CreateFailedException::class)
    override fun writeCurrentAttributes(instanceIdentifier: InstanceIdentifier<Config>, config: Config, writeContext: WriteContext) {
        // NOOP at this level
    }

    @Throws(WriteFailedException::class)
    override fun updateCurrentAttributes(id: InstanceIdentifier<Config>, dataBefore: Config, dataAfter: Config, writeContext: WriteContext) {
        // NOOP at this level
    }

    @Throws(WriteFailedException.DeleteFailedException::class)
    override fun deleteCurrentAttributes(instanceIdentifier: InstanceIdentifier<Config>, config: Config, writeContext: WriteContext) {
        // NOOP at this level
    }
}
