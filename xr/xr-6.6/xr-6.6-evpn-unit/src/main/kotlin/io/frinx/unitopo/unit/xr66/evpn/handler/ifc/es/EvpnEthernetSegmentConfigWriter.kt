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

package io.frinx.unitopo.unit.xr66.evpn.handler.ifc.es

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr66.evpn.handler.ifc.EvpnInterfaceListReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.EthernetSegmentIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.EthernetSegmentLoadBalance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.interfaces.evpn._interface.EthernetSegment
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.interfaces.evpn._interface.EthernetSegmentBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.identifier.IdentifierBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.HexInteger
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.ethernet.segment.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.types.rev181112.PORTACTIVE
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.interfaces.EvpnInterface as NativeEvpnInterface
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.interfaces.EvpnInterfaceBuilder as NativeEvpnInterfaceBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class EvpnEthernetSegmentConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: IID<Config>, dataAfter: Config, writeContext: WriteContext) {
        val name = id.firstKeyOf(Interface::class.java).name
        val underlayId = EvpnInterfaceListReader
            .getUnderlayInterfaceId(name)
        underlayAccess.merge(underlayId, NativeEvpnInterfaceBuilder().fromOpenConfig(name, dataAfter))
    }

    override fun deleteCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val underlayId = EvpnInterfaceListReader
            .getUnderlayInterfaceId(id.firstKeyOf(Interface::class.java).name)
            .child(EthernetSegment::class.java)
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
}

fun NativeEvpnInterfaceBuilder.fromOpenConfig(name: String, data: Config): NativeEvpnInterface {
    ethernetSegment = EthernetSegmentBuilder().fromOpenConfig(data)
    interfaceName = InterfaceName(name)
    return build()
}

fun EthernetSegmentBuilder.fromOpenConfig(data: Config): EthernetSegment {
    this.isEnable = true
    // translate identifier
    data.identifier?.let {
        identifier = IdentifierBuilder().apply {
            val values = it.value.split(":")
            this.bytes01 = HexInteger(values[0])
            this.bytes23 = HexInteger(values[1] + values[2])
            this.bytes45 = HexInteger(values[3] + values[4])
            this.bytes67 = HexInteger(values[5] + values[6])
            this.bytes89 = HexInteger(values[7] + values[8])
            this.type = EthernetSegmentIdentifier.Type0
        }.build()
    }

    // bgp rouer target
    esImportRouteTarget = data.bgpRouteTarget
    // load balancing mode
    loadBalancingMode = data.loadBalancingMode?.let {
        if (it.equals(PORTACTIVE::class.java)) {
            EthernetSegmentLoadBalance.PortActive
        } else {
            EthernetSegmentLoadBalance.SingleActive
        }
    }
    return build()
}