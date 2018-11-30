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

package io.frinx.unitopo.unit.xr7.evpn.handler.ifc.es

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.evpns.handler.EvpnGroupListReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.EthernetSegmentLoadBalance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.EvpnInterfaces
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.interfaces.evpn._interface.EthernetSegment
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.EthernetSegmentBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.ethernet.segment.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.ethernet.segment.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.types.rev181112.EthernetSegmentIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.types.rev181112.PORTACTIVE
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class EvpnEthernetSegmentConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(instanceIdentifier: IID<Config>) = ConfigBuilder()

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Config>,
        builder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val ifcId = EvpnGroupListReader.EVPN_TABLES
            .child(EvpnInterfaces::class.java)
        val name = instanceIdentifier.firstKeyOf(Interface::class.java).name
        underlayAccess.read(ifcId, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
            ?.let {
                it?.evpnInterface?.filter {
                    it.interfaceName == InterfaceName(name)
                }?.get(0).let {
                    it?.ethernetSegment?.let {
                        builder.fromUnderlay(it)
                    }
                }
            }
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as EthernetSegmentBuilder).config = config
    }
}

fun ConfigBuilder.fromUnderlay(es: EthernetSegment) {
    // load balancing mode
    loadBalancingMode = es.loadBalancingMode?.let {
        if (it.equals(EthernetSegmentLoadBalance.PortActive)) {
            PORTACTIVE::class.java
        } else {
            null
        }
    }

    // esi
    identifier = es.identifier?.let {
        EthernetSegmentIdentifier(StringBuilder()
            .append(it.bytes01.value)
            .append(split2Byte(it.bytes23.value))
            .append(split2Byte(it.bytes45.value))
            .append(split2Byte(it.bytes67.value))
            .append(split2Byte(it.bytes89.value))
            .toString())
    }

    // bgp route target
    bgpRouteTarget = es.esImportRouteTarget
}

fun ConfigBuilder.split2Byte(s: String): String {
    if (s.length == 2) {
        return ":00:" + s
    } else {
        return ":" + s.substring(0, 2) + ":" + s.substring(2)
    }
}