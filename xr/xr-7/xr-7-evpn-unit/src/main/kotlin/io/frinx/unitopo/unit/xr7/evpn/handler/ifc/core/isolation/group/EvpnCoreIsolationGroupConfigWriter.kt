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

package io.frinx.unitopo.unit.xr7.evpn.handler.ifc.core.isolation.group

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.evpns.handler.EvpnInterfaceListReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.EvpnCoreGroupIdRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.core.isolation.group.Config
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.interfaces.EvpnInterfaceBuilder as NativeEvpnInterfaceBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class EvpnCoreIsolationGroupConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: IID<Config>, dataAfter: Config, writeContext: WriteContext) {
        var ifcName = id.firstKeyOf(Interface::class.java).name
        val underlayId = EvpnInterfaceListReader.getUnderlayInterfaceId(ifcName)
        val builder = NativeEvpnInterfaceBuilder().apply {
            this.evpnCoreIsolationGroup = EvpnCoreGroupIdRange(dataAfter.id)
            this.interfaceName = InterfaceName(ifcName)
        }
        underlayAccess.merge(underlayId, builder.build())
    }

    override fun deleteCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val underlayId = EvpnInterfaceListReader
            .getUnderlayInterfaceId(id.firstKeyOf(Interface::class.java).name)

        val builder = underlayAccess.read(underlayId, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
            ?.let {
                NativeEvpnInterfaceBuilder(it)
            }

        builder?.setEvpnCoreIsolationGroup(null)?.let {
            underlayAccess.put(underlayId, it.build())
        }
    }

    override fun updateCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        var ifcName = id.firstKeyOf(Interface::class.java).name
        val underlayId = EvpnInterfaceListReader.getUnderlayInterfaceId(ifcName)
        val builder = NativeEvpnInterfaceBuilder().apply {
            this.evpnCoreIsolationGroup = EvpnCoreGroupIdRange(dataAfter.id)
            this.interfaceName = InterfaceName(ifcName)
        }
        underlayAccess.merge(underlayId, builder.build())
    }
}