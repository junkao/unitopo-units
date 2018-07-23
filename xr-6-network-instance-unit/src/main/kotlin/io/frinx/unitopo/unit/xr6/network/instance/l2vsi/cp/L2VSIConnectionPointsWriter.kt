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

package io.frinx.unitopo.unit.xr6.network.instance.l2vsi.cp

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.network.instance.common.L2vsiWriter
import io.frinx.unitopo.unit.xr6.network.instance.l2vsi.L2VSIReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.BgpRouteDistinguisher
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.BgpRouteTargetFormat
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.BgpRouteTargetRole
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.RdasIndex
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.RdasRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.VeidRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.VpnidRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.BridgeDomain
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.BridgeDomainBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.BridgeDomainKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.BdAttachmentCircuitsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.VfisBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.bd.attachment.circuits.BdAttachmentCircuit
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.bd.attachment.circuits.BdAttachmentCircuitBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.vfis.VfiBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.vfis.vfi.BgpAutoDiscoveryBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.vfis.vfi.bgp.auto.discovery.BgpSignalingProtocolBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.vfis.vfi.bgp.auto.discovery.RouteDistinguisherBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.vfis.vfi.bgp.auto.discovery.RouteTargetsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.vfis.vfi.bgp.auto.discovery.route.targets.RouteTargetBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.vfis.vfi.bgp.auto.discovery.route.targets.route.target.TwoByteAsOrFourByteAsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPoints
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.Endpoint
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.LOCAL
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class L2VSIConnectionPointsWriter(private val underlayAccess: UnderlayAccess) : L2vsiWriter<ConnectionPoints> {

    override fun writeCurrentAttributesForType(
        id: InstanceIdentifier<ConnectionPoints>,
        o: ConnectionPoints,
        writeContext: WriteContext
    ) {
        val l2vsiName = id.firstKeyOf(NetworkInstance::class.java).name

        val protocols = writeContext.readAfter(IIDs.NETWORKINSTANCES.child(NetworkInstance::class.java,
            NetworkInstanceKey(l2vsiName))
                .child(Protocols::class.java))
                .orNull()
        requireNotNull(protocols, { "No routing protocol set for l2vpn: $l2vsiName" })

        val bgpConfig = protocols!!.protocol.orEmpty()
                .firstOrNull { protocol -> protocol.identifier == BGP::class.java }
        requireNotNull(bgpConfig, { "BGP routing protocol needs to be set for l2vpn: $l2vsiName" })
        val asNumber = bgpConfig!!.bgp!!.global!!.config!!.`as`!!

        underlayAccess.merge(L2VSIReader.UNDERLAY_BD_ID
                .child(BridgeDomain::class.java, BridgeDomainKey(CiscoIosXrString(l2vsiName))),
                o.toUnderlay(l2vsiName, asNumber))
    }

    override fun updateCurrentAttributesForType(
        id: InstanceIdentifier<ConnectionPoints>,
        dataBefore: ConnectionPoints,
        dataAfter: ConnectionPoints,
        writeContext: WriteContext
    ) {
        deleteCurrentAttributes(id, dataBefore, writeContext)
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributesForType(
        id: InstanceIdentifier<ConnectionPoints>,
        o: ConnectionPoints,
        writeContext: WriteContext
    ) {
        val l2vsiName = id.firstKeyOf(NetworkInstance::class.java).name

        underlayAccess.delete(L2VSIReader.UNDERLAY_BD_ID
                .child(BridgeDomain::class.java, BridgeDomainKey(CiscoIosXrString(l2vsiName))))
    }
}

private fun ConnectionPoints.toUnderlay(l2vsiName: String, asNumber: AsNumber): BridgeDomain {

    requireNotNull(connectionPoint, { "No connection points set for l2vpn: $l2vsiName" })
    require(!connectionPoint!!.isEmpty(), { "No connection points set for l2vpn: $l2vsiName" })

    val remotePoint = connectionPoint.orEmpty()
            .firstOrNull { cp -> cp.connectionPointId == L2VSIConnectionPointsReader.REMOTE_ID }

    requireNotNull(remotePoint, { "Remote point not set for l2vpn: $l2vsiName" })
    val vccid = remotePoint!!.endpoints.endpoint.orEmpty().first().remote.config.virtualCircuitIdentifier

    val builder = BridgeDomainBuilder()

    builder.setName(CiscoIosXrString(l2vsiName)).vfis = VfisBuilder()
            .setVfi(listOf(VfiBuilder()
                    .setName(CiscoIosXrString(l2vsiName))
                    .setVpnid(VpnidRange(vccid))
                    .setBgpAutoDiscovery(BgpAutoDiscoveryBuilder()
                            .setEnable(true)
                            .setRouteDistinguisher(RouteDistinguisherBuilder()
                                    .setType(BgpRouteDistinguisher.Auto)
                                    .build())
                            .setRouteTargets(RouteTargetsBuilder()
                                    .setRouteTarget(listOf(RouteTargetBuilder()
                                            .setRole(BgpRouteTargetRole.Both)
                                            .setFormat(BgpRouteTargetFormat.TwoByteAs)
                                            .setTwoByteAsOrFourByteAs(listOf(TwoByteAsOrFourByteAsBuilder()
                                                    .setAs(RdasRange(asNumber.value))
                                                    .setAsIndex(RdasIndex(vccid))
                                                    .build()))
                                            .build()))
                                    .build())
                            .setBgpSignalingProtocol(BgpSignalingProtocolBuilder()
                                    .setEnable(true)
                                    .setVeid(VeidRange(L2VSIConnectionPointsReader.VE_ID))
                                    .build())
                            .build())
                    .build()))
            .build()

    connectionPoint.orEmpty()
            .filter { cp -> cp.endpoints.endpoint.orEmpty().first().config.type == LOCAL::class.java }
            .map { cp -> cp.endpoints.endpoint.orEmpty().first() }
            .map { cp -> cp.toUnderlayLocalEndpoint() }
            .let { builder.setBdAttachmentCircuits(BdAttachmentCircuitsBuilder()
                    .setBdAttachmentCircuit(it)
                    .build()) }

    return builder.build()
}

private fun Endpoint.toUnderlayLocalEndpoint(): BdAttachmentCircuit {

    val ifcName = if (local!!.config!!.subinterface == null) {
        local!!.config.`interface`
    } else {
        "${local!!.config.`interface`}.${local!!.config.subinterface}"
    }

    return BdAttachmentCircuitBuilder()
            .setName(InterfaceName(ifcName))
            .build()
}