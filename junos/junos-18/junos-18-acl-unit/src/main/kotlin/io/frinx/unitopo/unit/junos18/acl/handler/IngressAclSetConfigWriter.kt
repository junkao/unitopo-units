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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.ACLIPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.ingress.acl.top.ingress.acl.sets.ingress.acl.set.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.filter.input_choice.case_1.Input
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.filter.input_choice.case_1.InputBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class IngressAclSetConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: IID<Config>, dataAfter: Config, writeContext: WriteContext) {
        require(dataAfter.type == ACLIPV4::class.java) {
            "Unknown acl type is specified: name=${dataAfter.setName}, type=${dataAfter.type?.simpleName}"
        }

        val (underlayId, underlayIfcCfg) = getData(id, dataAfter)

        underlayAccess.merge(underlayId, underlayIfcCfg)
    }

    override fun updateCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        // Both type and set-name attributes are keys in parent container.
        // So this container has no modifiable attributes.
    }

    override fun deleteCurrentAttributes(id: IID<Config>, dataBefore: Config, writeContext: WriteContext) {
        val underlayId = AclInterfaceReader.getUnderlayIpv4FilterId(id.firstKeyOf(Interface::class.java).id.value)
            .child(Input::class.java)

        underlayAccess.delete(underlayId)
    }

    private fun getData(id: IID<Config>, dataAfter: Config): Pair<IID<Input>, Input> {
        val underlayId = AclInterfaceReader.getUnderlayIpv4FilterId(id.firstKeyOf(Interface::class.java).id.value)
            .child(Input::class.java)

        val filterData = InputBuilder()
            .setFilterName(dataAfter.setName)
            .build()
        return Pair(underlayId, filterData)
    }
}