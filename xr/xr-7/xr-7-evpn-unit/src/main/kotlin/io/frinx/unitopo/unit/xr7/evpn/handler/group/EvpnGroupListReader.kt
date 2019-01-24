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

package io.frinx.unitopo.unit.xr7.evpns.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.Evpn
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.EvpnGroupIdRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.EvpnTables
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.EvpnGroups
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.GroupsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.Group
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.GroupBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.GroupKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.groups.EvpnGroup as NativeEvpnGroup
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.groups.EvpnGroupKey as NativeEvpnGroupKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class EvpnGroupListReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Group, GroupKey, GroupBuilder> {

    override fun getBuilder(instanceIdentifier: IID<Group>) = GroupBuilder()

    override fun getAllIds(instanceIdentifier: IID<Group>, readContext: ReadContext): List<GroupKey> {
        return underlayAccess.read(EVPN_TABLES, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
            ?.let {
                parseEvpnGroupIds(it)
            }.orEmpty()
    }

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Group>,
        builder: GroupBuilder,
        readContext: ReadContext
    ) {
        // this reader only read all evpn groups with id
        builder.setId(instanceIdentifier.firstKeyOf(Group::class.java).id)
        builder.setConfig(ConfigBuilder().setId(builder.id).build())
    }

    override fun merge(builder: Builder<out DataObject>, list: List<Group>) {
        (builder as GroupsBuilder).setGroup(list)
    }

    companion object {
        val NATIVE_EVPN = IID.create(Evpn::class.java)
        val EVPN_TABLES = NATIVE_EVPN.child(EvpnTables::class.java)!!

        @VisibleForTesting
        fun parseEvpnGroupIds(it: EvpnTables) = it.evpnGroups?.evpnGroup.orEmpty()
            .mapNotNull {
                GroupKey(it.groupId.value)
            }.toList()

        fun getUnderlayGroupId(id: Long) = EVPN_TABLES.child(EvpnGroups::class.java)
            .child(NativeEvpnGroup::class.java,
                NativeEvpnGroupKey(EvpnGroupIdRange(id)))
    }
}