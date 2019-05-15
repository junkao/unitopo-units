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

package io.frinx.unitopo.ifc.base.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

abstract class AbstractInterfaceReader<T : DataObject>(protected val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Interface>,
        interfaceBuilder: InterfaceBuilder,
        readContext: ReadContext
    ) {
        interfaceBuilder.name = instanceIdentifier.firstKeyOf(Interface::class.java).name
    }

    override fun getAllIds(instanceIdentifier: InstanceIdentifier<Interface>, readContext: ReadContext):
        List<InterfaceKey> = getInterfaceIds()

    fun getInterfaceIds(): List<InterfaceKey> =
        underlayAccess.read(readIid, readDSType)
        .checkedGet()
        .orNull()
        ?.let { parseInterfaceIds(it) }.orEmpty()

    abstract fun parseInterfaceIds(data: T): List<InterfaceKey>

    abstract val readIid: InstanceIdentifier<T>

    open val readDSType: LogicalDatastoreType = LogicalDatastoreType.CONFIGURATION
}