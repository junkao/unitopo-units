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

package io.frinx.unitopo.unit.xr7.evpn.handler.group

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.evpns.handler.EvpnGroupListReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.EvpnGroupIdRange
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.Group
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.GroupKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.groups.EvpnGroupBuilder as NativeEvpnGroupBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class EvpnGroupListWriter(private val underlayAccess: UnderlayAccess) : ListWriterCustomizer<Group, GroupKey> {

    override fun writeCurrentAttributes(id: IID<Group>, dataAfter: Group, writeContext: WriteContext) {
        val underlayId = EvpnGroupListReader.getUnderlayGroupId(dataAfter.id)
        val builder = NativeEvpnGroupBuilder().apply {
            this.groupId = EvpnGroupIdRange(dataAfter.id)
        }
        underlayAccess.put(underlayId, builder.build())
    }

    override fun deleteCurrentAttributes(
        id: IID<Group>,
        dataBefore: Group,
        writeContext: WriteContext
    ) {
        val underlayId = EvpnGroupListReader.getUnderlayGroupId(dataBefore.id)
        underlayAccess.delete(underlayId)
    }

    override fun updateCurrentAttributes(
        id: IID<Group>,
        dataBefore: Group,
        dataAfter: Group,
        writeContext: WriteContext
    ) {
        // no processing only for group id changed
    }
}