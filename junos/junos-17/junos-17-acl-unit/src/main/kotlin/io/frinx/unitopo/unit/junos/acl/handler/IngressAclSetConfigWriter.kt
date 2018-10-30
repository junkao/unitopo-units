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
package io.frinx.unitopo.unit.junos.acl.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.ingress.acl.top.ingress.acl.sets.ingress.acl.set.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.Filter
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.FilterBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.filter.input_choice.Case1Builder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.filter.input_choice.case_1.Input
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.filter.input_choice.case_1.InputBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class IngressAclSetConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: IID<Config>, dataAfter: Config, writeContext: WriteContext) {
        writeData(id, dataAfter)
    }

    override fun deleteCurrentAttributes(id: IID<Config>, dataBefore: Config, writeContext: WriteContext) {
        val underlayId = AclInterfaceReader.getUnderlayFilterId(id.firstKeyOf(Interface::class.java).id.value)
                .child(Input::class.java)

        try {
            underlayAccess.delete(underlayId)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun writeData(id: IID<Config>, data: Config) {
        val (underlayId, underlayIfcCfg) = getData(id, data)

        try {
            underlayAccess.merge(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(id: IID<Config>, dataAfter: Config):
            Pair<IID<Filter>, Filter> {
        val underlayId = AclInterfaceReader.getUnderlayFilterId(id.firstKeyOf(Interface::class.java).id.value)

        val filterData = FilterBuilder()
                .setInputChoice(Case1Builder()
                        .setInput(InputBuilder()
                            .setFilterName(dataAfter.setName)
                            .build())
                        .build())
                .build()
        return Pair(underlayId, filterData)
    }
}