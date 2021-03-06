/*
 * Copyright © 2019 Frinx and others.
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

package io.frinx.unitopo.unit.xr6.lr.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class StaticProtocolWriter : CompositeWriter.Child<Config> {

    override fun updateCurrentAttributesWResult
        (iid: InstanceIdentifier<Config>, dataBefore: Config, dataAfter: Config, writeContext: WriteContext): Boolean {
        return ChecksMap.PathCheck.Protocol.STATIC_ROUTING.canProcess(iid, writeContext, false)
    }

    override fun writeCurrentAttributesWResult
        (iid: InstanceIdentifier<Config>, data: Config, writeContext: WriteContext): Boolean {
        return ChecksMap.PathCheck.Protocol.STATIC_ROUTING.canProcess(iid, writeContext, false)
    }

    override fun deleteCurrentAttributesWResult
        (iid: InstanceIdentifier<Config>, dataBefore: Config, writeContext: WriteContext): Boolean {
        return ChecksMap.PathCheck.Protocol.STATIC_ROUTING.canProcess(iid, writeContext, true)
    }
}