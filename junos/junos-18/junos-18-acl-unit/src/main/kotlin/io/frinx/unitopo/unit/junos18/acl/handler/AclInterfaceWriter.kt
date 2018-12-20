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

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.Interface as AclInterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.InterfaceKey as AclInterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class AclInterfaceWriter : ListWriterCustomizer<AclInterface, AclInterfaceKey> {
    override fun writeCurrentAttributes(id: IID<AclInterface>, dataAfter: AclInterface, writeContext: WriteContext) {
        val interfaceName = id.firstKeyOf(AclInterface::class.java).id.value!!

        // check logic only
        require(AclInterfaceReader.INTERFACE_ID_PATTERN.matcher(interfaceName).matches()) {
            "Interface name does not match '<interface>.<unit>'. id=$interfaceName"
        }
    }

    override fun updateCurrentAttributes(
        id: IID<AclInterface>,
        dataBefore: AclInterface,
        dataAfter: AclInterface,
        writeContext: WriteContext
    ) {
        // NOP
    }

    override fun deleteCurrentAttributes(id: IID<AclInterface>, dataBefore: AclInterface, writeContext: WriteContext) {
        // NOP
    }
}