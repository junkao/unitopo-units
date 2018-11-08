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

package io.frinx.unitopo.unit.junos.lacp.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader.Companion.IFCS
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.aggregation.lacp.members.top.MembersBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.aggregation.lacp.members.top.members.Member
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.aggregation.lacp.members.top.members.MemberBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.aggregation.lacp.members.top.members.MemberKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.lacp.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Interfaces
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class MemberReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Member, MemberKey, MemberBuilder> {

    override fun readCurrentAttributes(iid: InstanceIdentifier<Member>, builder: MemberBuilder, context: ReadContext) {
        val ifcId = iid.firstKeyOf(Member::class.java).`interface`
        builder.key = MemberKey(ifcId)
    }

    override fun getAllIds(iid: InstanceIdentifier<Member>, context: ReadContext): List<MemberKey> {
        val bundleId = iid.firstKeyOf(Interface::class.java).name
        return underlayAccess.read(IFCS, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
            ?.let { parseInterfaceIds(bundleId, it) }.orEmpty()
    }

    override fun merge(builder: Builder<out DataObject>, members: MutableList<Member>) {
        (builder as MembersBuilder).member = members
    }

    override fun getBuilder(id: InstanceIdentifier<Member>): MemberBuilder = MemberBuilder()

    companion object {
        private fun parseInterfaceIds(bundleId: String, it: Interfaces): List<MemberKey> {
            return it.`interface`.orEmpty()
                .filter { port -> port.gigetherOptions?.ieee8023ad?.bundle?.interfaceDevice?.value
                    .equals(bundleId).or(false) }
                .map { it.key }
                .map { MemberKey(it.name) }
        }
    }
}