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

package io.frinx.unitopo.ifc.base.handler.subinterfaces

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

abstract class AbstractSubinterfaceReader<T : DataObject>(protected val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Subinterface, SubinterfaceKey, SubinterfaceBuilder> {

    override fun getAllIds(iid: InstanceIdentifier<Subinterface>, context: ReadContext): List<SubinterfaceKey> {
        val ifcName = iid.firstKeyOf(Interface::class.java).name
        return underlayAccess.read(readIid(ifcName), readDSType)
            .checkedGet().orNull()?.let {
                parseSubInterfaceIds(it, ifcName)
            }.orEmpty()
    }

    abstract fun readIid(ifcName: String): InstanceIdentifier<T>
    open val readDSType: LogicalDatastoreType = LogicalDatastoreType.CONFIGURATION

    abstract fun parseSubInterfaceIds(data: T, ifcName: String): List<SubinterfaceKey>

    override fun readCurrentAttributes(
        iid: InstanceIdentifier<Subinterface>,
        builder: SubinterfaceBuilder,
        context: ReadContext
    ) {
        builder.index = iid.firstKeyOf(Subinterface::class.java).index
    }
}