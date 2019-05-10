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

package io.frinx.unitopo.ni.base.handler.pf

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

abstract class AbstractPolicyForwardingInterfaceConfigWriter
    <T : DataObject>(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    abstract fun getUnderlayIid(iid: InstanceIdentifier<Config>): InstanceIdentifier<T>

    abstract fun getData(data: Config): T

    override fun writeCurrentAttributes(iid: InstanceIdentifier<Config>, dataAfter: Config, ctx: WriteContext) {
        requires(iid)
        underlayAccess.safePut(getUnderlayIid(iid), getData(dataAfter))
    }

    override fun updateCurrentAttributes(
        iid: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        ctx: WriteContext
    ) {
        underlayAccess.safeMerge(getUnderlayIid(iid), getData(dataBefore), getUnderlayIid(iid), getData(dataAfter))
    }

    override fun deleteCurrentAttributes(iid: InstanceIdentifier<Config>, dataBefore: Config, ctx: WriteContext) {
        requires(iid)
        underlayAccess.delete(getUnderlayIid(iid))
    }

    abstract fun requires(id: InstanceIdentifier<Config>)
}