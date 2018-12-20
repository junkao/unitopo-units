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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.ACLIPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.ingress.acl.top.IngressAclSetsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.ingress.acl.top.ingress.acl.sets.IngressAclSet
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.ingress.acl.top.ingress.acl.sets.IngressAclSetBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.ingress.acl.top.ingress.acl.sets.IngressAclSetKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import java.util.Collections
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.Interface as AclInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.filter.input_choice.Case1 as Ipv4InputChoiscCase1
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class IngressAclSetReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<IngressAclSet, IngressAclSetKey, IngressAclSetBuilder> {

    override fun getBuilder(id: IID<IngressAclSet>): IngressAclSetBuilder = IngressAclSetBuilder()

    override fun merge(builder: Builder<out DataObject>, sets: List<IngressAclSet>) {
        (builder as IngressAclSetsBuilder).ingressAclSet = sets
    }

    override fun getAllIds(id: IID<IngressAclSet>, readContext: ReadContext): List<IngressAclSetKey> {
        return readIpv4Ids(id.firstKeyOf(AclInterface::class.java).id.value)
    }

    override fun readCurrentAttributes(
        id: IID<IngressAclSet>,
        builder: IngressAclSetBuilder,
        readContext: ReadContext
    ) {
        val setKey = id.firstKeyOf(IngressAclSet::class.java)
        builder.key = setKey
        builder.type = setKey.type
        builder.setName = setKey.setName
    }

    private fun readIpv4Ids(ifcName: String): List<IngressAclSetKey> {
        val filter = underlayAccess.read(AclInterfaceReader.getUnderlayIpv4FilterId(ifcName),
            LogicalDatastoreType.CONFIGURATION)
                .checkedGet().orNull()

        return if (filter?.inputChoice != null) {
            Collections.singletonList(IngressAclSetKey((filter.inputChoice as Ipv4InputChoiscCase1)
                .input?.filterName, ACLIPV4::class.java))
        } else {
            emptyList()
        }
    }
}