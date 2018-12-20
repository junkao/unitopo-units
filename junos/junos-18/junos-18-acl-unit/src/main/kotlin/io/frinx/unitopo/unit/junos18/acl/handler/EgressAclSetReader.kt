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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.EgressAclSetsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.egress.acl.sets.EgressAclSet
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.egress.acl.sets.EgressAclSetBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.egress.acl.sets.EgressAclSetKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import java.util.Collections
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.Interface as AclInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.filter.output_choice.Case1 as Ipv4OutputChoiceCase1
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class EgressAclSetReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<EgressAclSet, EgressAclSetKey, EgressAclSetBuilder> {

    override fun getBuilder(id: IID<EgressAclSet>): EgressAclSetBuilder = EgressAclSetBuilder()

    override fun merge(builder: Builder<out DataObject>, sets: List<EgressAclSet>) {
        (builder as EgressAclSetsBuilder).egressAclSet = sets
    }

    override fun getAllIds(id: IID<EgressAclSet>, readContext: ReadContext): List<EgressAclSetKey> {
        return readIpv4Ids(id.firstKeyOf(AclInterface::class.java).id.value)
    }

    override fun readCurrentAttributes(
        id: IID<EgressAclSet>,
        builder: EgressAclSetBuilder,
        readContext: ReadContext
    ) {
        val setKey = id.firstKeyOf(EgressAclSet::class.java)
        builder.key = setKey
        builder.type = setKey.type
        builder.setName = setKey.setName
    }

    private fun readIpv4Ids(ifcName: String): List<EgressAclSetKey> {
        val filter = underlayAccess.read(AclInterfaceReader.getUnderlayIpv4FilterId(ifcName),
            LogicalDatastoreType.CONFIGURATION)
            .checkedGet().orNull()

        return if (filter?.outputChoice != null) {
            Collections.singletonList(EgressAclSetKey((filter.outputChoice as Ipv4OutputChoiceCase1)
                .output?.filterName, ACLIPV4::class.java))
        } else {
            emptyList()
        }
    }
}