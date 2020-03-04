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

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.evpn.handler.group.EvpnGroupListReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.EvpnGroupIdRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.EvpnGroups
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.EvpnGroup
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.EvpnGroupKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.evpn.group.EvpnGroupCoreInterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.Group
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.CoreInterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.core.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.core.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.core.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn._interface.part.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class EvpnGroupCoreInterfaceListReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    override fun getBuilder(instanceIdentifier: IID<Interface>) = InterfaceBuilder()

    override fun getAllIds(instanceIdentifier: IID<Interface>, readContext: ReadContext): List<InterfaceKey> {
        return underlayAccess.read(getUnderlayCoreInterfaceId(instanceIdentifier),
            LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
            ?.let {
                it.evpnGroupCoreInterface.orEmpty()
                    .map {
                        InterfaceKey(it.interfaceName.value)
                    }.toList()
            }.orEmpty()
    }

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Interface>,
        builder: InterfaceBuilder,
        readContext: ReadContext
    ) {
        val ifcName = instanceIdentifier.firstKeyOf(Interface::class.java).name
        builder.setName(ifcName)
        builder.setConfig(ConfigBuilder().apply {
            name = ifcName
        }.build())
    }

    override fun merge(builder: Builder<out DataObject>, list: List<Interface>) {
        (builder as CoreInterfacesBuilder).`interface` = list
    }

    private fun getUnderlayCoreInterfaceId(id: IID<Interface>):
        IID<EvpnGroupCoreInterfaces> {
        val groupId = id.firstKeyOf(Group::class.java).id
        return EvpnGroupListReader.EVPN_TABLES.child(EvpnGroups::class.java)
            .child(EvpnGroup::class.java, EvpnGroupKey(EvpnGroupIdRange(groupId)))
            .child(EvpnGroupCoreInterfaces::class.java)
    }
}