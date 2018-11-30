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

package io.frinx.unitopo.unit.xr7.evpn.handler.ifc

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.evpns.handler.EvpnInterfaceListReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.interfaces.EvpnInterfaceBuilder as NativeEvpnInterfaceBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class EvpnInterfaceListWriter(private val underlayAccess: UnderlayAccess) :
    ListWriterCustomizer<Interface, InterfaceKey> {

    override fun writeCurrentAttributes(id: IID<Interface>, dataAfter: Interface, writeContext: WriteContext) {
        val underlayId = EvpnInterfaceListReader.getUnderlayInterfaceId(dataAfter)
        val builder = NativeEvpnInterfaceBuilder().apply {
            this.interfaceName = InterfaceName(dataAfter.name)
        }
        underlayAccess.put(underlayId, builder.build())
    }

    override fun deleteCurrentAttributes(
        id: IID<Interface>,
        dataBefore: Interface,
        writeContext: WriteContext
    ) {
        val underlayId = EvpnInterfaceListReader.getUnderlayInterfaceId(dataBefore)
        underlayAccess.delete(underlayId)
    }

    override fun updateCurrentAttributes(
        id: IID<Interface>,
        dataBefore: Interface,
        dataAfter: Interface,
        writeContext: WriteContext
    ) {
        val underlayId = EvpnInterfaceListReader.getUnderlayInterfaceId(dataAfter)
        val builder = NativeEvpnInterfaceBuilder().apply {
            this.interfaceName = InterfaceName(dataAfter.name)
        }
        underlayAccess.merge(underlayId, builder.build())
    }
}