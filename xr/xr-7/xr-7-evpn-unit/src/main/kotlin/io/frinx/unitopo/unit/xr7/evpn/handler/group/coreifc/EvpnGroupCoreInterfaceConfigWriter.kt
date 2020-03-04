/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.evpn.handler.group.coreifc

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.evpn.handler.group.EvpnGroupListReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.EvpnGroupIdRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.EvpnGroupKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.evpn.group.EvpnGroupCoreInterfaces
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.evpn.group.evpn.group.core.interfaces.EvpnGroupCoreInterface
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.evpn.group.evpn.group.core.interfaces.EvpnGroupCoreInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.evpn.group.evpn.group.core.interfaces.EvpnGroupCoreInterfaceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn._interface.part.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.Group
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.EvpnGroups as NativeEvpnGroups
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.EvpnGroup as NativeEvpnGroup
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class EvpnGroupCoreInterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: IID<Config>, dataAfter: Config, writeContext: WriteContext) {
        val underlayId = getUnderlayCoreInterfaceId(id, dataAfter)
        val builder = EvpnGroupCoreInterfaceBuilder().apply {
            this.interfaceName = InterfaceName(dataAfter.name)
        }
        underlayAccess.put(underlayId, builder.build())
    }

    override fun deleteCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val underlayId = getUnderlayCoreInterfaceId(id, dataBefore)
        underlayAccess.delete(underlayId)
    }

    override fun updateCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

    private fun getUnderlayCoreInterfaceId(id: IID<Config>, data: Config):
        KeyedInstanceIdentifier<EvpnGroupCoreInterface, EvpnGroupCoreInterfaceKey> {
        val groupId = id.firstKeyOf(Group::class.java).id
        return EvpnGroupListReader.EVPN_TABLES.child(NativeEvpnGroups::class.java)
            .child(NativeEvpnGroup::class.java, EvpnGroupKey(EvpnGroupIdRange(groupId)))
            .child(EvpnGroupCoreInterfaces::class.java)
            .child(EvpnGroupCoreInterface::class.java, EvpnGroupCoreInterfaceKey(InterfaceName(data.name)))
    }
}