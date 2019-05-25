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

package io.frinx.unitopo.ni.base.handler.vrf.def

import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.translate.unit.commons.handler.spi.CompositeWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.DEFAULTINSTANCE
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class DefaultConfigWriter : CompositeWriter.Child<Config> {

    @Throws(WriteFailedException.CreateFailedException::class)
    override fun writeCurrentAttributesWResult(
        instanceIdentifier: InstanceIdentifier<Config>,
        config: Config,
        writeContext: WriteContext
    ): Boolean {
        if (config.type == DEFAULTINSTANCE::class.java) {
            throw WriteFailedException.CreateFailedException(instanceIdentifier, config, EX)
        }
        return false
    }

    @Throws(WriteFailedException.UpdateFailedException::class)
    override fun updateCurrentAttributesWResult(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ): Boolean {
        if (dataAfter.type == DEFAULTINSTANCE::class.java) {
            throw WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, EX)
        }
        return false
    }

    @Throws(WriteFailedException.DeleteFailedException::class)
    override fun deleteCurrentAttributesWResult(
        instanceIdentifier: InstanceIdentifier<Config>,
        config: Config,
        writeContext: WriteContext
    ): Boolean {
        if (config.type == DEFAULTINSTANCE::class.java) {
            throw WriteFailedException.DeleteFailedException(instanceIdentifier, EX)
        }
        return false
    }

    companion object {
        private val EX = IllegalArgumentException("Default network instance cannot be manipulated")
    }
}