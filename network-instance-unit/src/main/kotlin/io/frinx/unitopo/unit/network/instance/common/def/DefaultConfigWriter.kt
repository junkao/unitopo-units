/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.network.instance.common.def

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.DEFAULTINSTANCE
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class DefaultConfigWriter : WriterCustomizer<Config> {

    @Throws(WriteFailedException.CreateFailedException::class)
    override fun writeCurrentAttributes(instanceIdentifier: InstanceIdentifier<Config>, config: Config, writeContext: WriteContext) {

        if (config.type == DEFAULTINSTANCE::class.java) {
            throw WriteFailedException.CreateFailedException(instanceIdentifier, config, EX)
        }
    }

    @Throws(WriteFailedException::class)
    override fun updateCurrentAttributes(id: InstanceIdentifier<Config>, dataBefore: Config, dataAfter: Config, writeContext: WriteContext) {

        if (dataAfter.type == DEFAULTINSTANCE::class.java) {
            throw WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, EX)
        }
    }

    @Throws(WriteFailedException.DeleteFailedException::class)
    override fun deleteCurrentAttributes(instanceIdentifier: InstanceIdentifier<Config>, config: Config, writeContext: WriteContext) {

        if (config.type == DEFAULTINSTANCE::class.java) {
            throw WriteFailedException.DeleteFailedException(instanceIdentifier, EX)
        }
    }

    companion object {

        private val EX = IllegalArgumentException("Default network instance cannot be manipulated")
    }
}
