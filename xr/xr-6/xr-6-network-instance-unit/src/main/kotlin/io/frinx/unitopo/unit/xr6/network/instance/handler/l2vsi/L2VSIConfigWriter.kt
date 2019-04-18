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

package io.frinx.unitopo.unit.xr6.network.instance.handler.l2vsi

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class L2VSIConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    @Throws(WriteFailedException.CreateFailedException::class)
    override fun writeCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Config>,
        config: Config,
        writeContext: WriteContext
    ) {
        // NOOP at this level
    }

    @Throws(WriteFailedException::class)
    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        // NOOP at this level
    }

    @Throws(WriteFailedException.DeleteFailedException::class)
    override fun deleteCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Config>,
        config: Config,
        writeContext: WriteContext
    ) {
        // NOOP at this level
    }
}