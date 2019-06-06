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

package io.frinx.unitopo.unit.xr66.bgp.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

// This Writer is necessary because an exception occurs
// if there is no writer(child of CompositeWriter) that handles data of Bgp.
class BgpProtocolWriter(private val underlayAccess: UnderlayAccess) : CompositeWriter.Child<Config> {

    override fun updateCurrentAttributesWResult(
        iid: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ): Boolean {
        if (!ChecksMap.PathCheck.Protocol.BGP.canProcess(iid, writeContext, false)) {
            return false
        }
        // NOOP
        return true
    }

    override fun writeCurrentAttributesWResult(id: IID<Config>, dataAfter: Config, wtx: WriteContext): Boolean {
        if (!ChecksMap.PathCheck.Protocol.BGP.canProcess(id, wtx, false)) {
            return false
        }
        // NOOP
        return true
    }

    override fun deleteCurrentAttributesWResult(id: IID<Config>, dataBefore: Config, wtx: WriteContext): Boolean {
        if (!ChecksMap.PathCheck.Protocol.BGP.canProcess(id, wtx, true)) {
            return false
        }
        // NOOP
        return true
    }
}