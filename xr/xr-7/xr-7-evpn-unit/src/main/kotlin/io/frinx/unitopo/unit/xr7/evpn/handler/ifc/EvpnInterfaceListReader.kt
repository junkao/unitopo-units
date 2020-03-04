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

package io.frinx.unitopo.unit.xr7.evpn.handler.ifc

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.evpn.handler.group.EvpnGroupListReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.EvpnInterfaces
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn._interface.part.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.InterfaceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.interfaces.EvpnInterface as NativeEvpnInterface
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.interfaces.EvpnInterfaceKey as NativeEvpnInterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class EvpnInterfaceListReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    override fun getBuilder(instanceIdentifier: IID<Interface>) = InterfaceBuilder()

    override fun getAllIds(instanceIdentifier: IID<Interface>, readContext: ReadContext): List<InterfaceKey> {
        return underlayAccess.read(getUnderlayInterfacesId(), LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
            ?.let {
                it.evpnInterface.orEmpty()
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
        val name = instanceIdentifier.firstKeyOf(Interface::class.java).name
        builder.setName(name)
        builder.setConfig(ConfigBuilder().apply {
            this.name = name
        }.build())
    }

    override fun merge(builder: Builder<out DataObject>, list: List<Interface>) {
        (builder as InterfacesBuilder).`interface` = list
    }

    private fun getUnderlayInterfacesId() =
        EvpnGroupListReader.EVPN_TABLES.child(EvpnInterfaces::class.java)

    companion object {
        fun getUnderlayInterfaceId(data: Interface) = EvpnGroupListReader.EVPN_TABLES
            .child(EvpnInterfaces::class.java)
            .child(NativeEvpnInterface::class.java,
                NativeEvpnInterfaceKey(InterfaceName(data.name)))

        fun getUnderlayInterfaceId(ifcName: String) = EvpnGroupListReader.EVPN_TABLES
            .child(EvpnInterfaces::class.java)
            .child(NativeEvpnInterface::class.java,
                NativeEvpnInterfaceKey(InterfaceName(ifcName)))
    }
}