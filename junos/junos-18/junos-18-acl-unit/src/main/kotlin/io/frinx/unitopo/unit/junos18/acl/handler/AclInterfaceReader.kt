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

package io.frinx.unitopo.unit.junos18.acl.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.Configuration1
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.Interfaces
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.root.rev180101.Configuration
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import java.util.regex.Pattern
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.Interface as AclInterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.InterfaceBuilder as AclInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.InterfaceKey as AclInterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class AclInterfaceReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<AclInterface, AclInterfaceKey, AclInterfaceBuilder> {

    override fun getBuilder(id: IID<AclInterface>): AclInterfaceBuilder = AclInterfaceBuilder()

    override fun merge(builder: Builder<out DataObject>, iface: List<AclInterface>) {
        (builder as InterfacesBuilder).`interface` = iface
    }

    override fun getAllIds(id: IID<AclInterface>, readContext: ReadContext): List<AclInterfaceKey> {
        return readInterfaceIds()
    }

    override fun readCurrentAttributes(id: IID<AclInterface>, builder: InterfaceBuilder, readContext: ReadContext) {
        val ifaceKey = id.firstKeyOf(AclInterface::class.java)
        builder.key = ifaceKey
        builder.id = ifaceKey.id
    }

    private fun readInterfaceIds(): List<AclInterfaceKey> {
        return underlayAccess.read(JUNOS_IFCS, LogicalDatastoreType.CONFIGURATION)
            .checkedGet().orNull()
            ?.`interface`.orEmpty()
            .flatMap { ifc ->
                ifc.unit.orEmpty()
                    .filter { it?.family?.inet?.filter != null }
                    .map { AclInterfaceKey(InterfaceId("${ifc.name}.${it.name}")) }
            }
    }

    companion object {
        private val JUNOS_CFG = IID.create(Configuration::class.java)!!
        private val JUNOS_IFCS_AUG = JUNOS_CFG.augmentation(Configuration1::class.java)!!
        val JUNOS_IFCS = JUNOS_IFCS_AUG.child(Interfaces::class.java)!!

        val INTERFACE_ID_PATTERN = Pattern.compile("(?<interface>[^\\.]+)\\.(?<unit>.*)")
    }
}