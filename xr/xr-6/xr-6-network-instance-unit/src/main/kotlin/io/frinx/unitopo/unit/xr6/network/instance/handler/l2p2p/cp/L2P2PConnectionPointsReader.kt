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

package io.frinx.unitopo.unit.xr6.network.instance.handler.l2p2p.cp

import com.google.common.collect.Lists
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.Reader
import io.fd.honeycomb.translate.spi.builder.BasicCheck
import io.fd.honeycomb.translate.spi.builder.Check
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.Util
import io.frinx.unitopo.unit.xr6.network.instance.handler.l2p2p.L2P2PReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnectKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.attachment.circuits.AttachmentCircuit
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.Pseudowire
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPoints
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPointsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.ConnectionPoint
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.ConnectionPointBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.EndpointsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.Endpoint
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.EndpointBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.endpoint.Local
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.endpoint.LocalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.endpoint.RemoteBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.LOCAL
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.REMOTE
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.Collections
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.ConfigBuilder as ConnectionPointConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.endpoint.ConfigBuilder as EndpointConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.endpoint.local.ConfigBuilder as LocalConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.endpoint.remote.ConfigBuilder as RemoteConfigBuilder

// TODO read also operational data
class L2P2PConnectionPointsReader(private val underlayAccess: UnderlayAccess) :
    CompositeReader.Child<ConnectionPoints, ConnectionPointsBuilder> {

    override fun getCheck(): Check {
        return BasicCheck.checkData(ChecksMap.DataCheck.NetworkInstanceConfig.IID_TRANSFORMATION,
            ChecksMap.DataCheck.NetworkInstanceConfig.TYPE_L2P2P)
    }

    override fun getBuilder(p0: InstanceIdentifier<ConnectionPoints>): ConnectionPointsBuilder {
        // NOOP
        throw UnsupportedOperationException("Should not be invoked")
    }

    override fun readCurrentAttributes(
        id: InstanceIdentifier<ConnectionPoints>,
        builder: ConnectionPointsBuilder,
        ctx: ReadContext
    ) {
        val l2p2InstanceName = id.firstKeyOf(NetworkInstance::class.java).name

        val underlayP2PConnectId = L2P2PReader.UNDERLAY_P2PXCONNECT_ID.child(P2pXconnect::class.java,
                P2pXconnectKey(CiscoIosXrString(l2p2InstanceName)))

        underlayAccess.read(underlayP2PConnectId)
                .checkedGet().orNull()
                ?.let { parseConnectionPoints(it, builder) }
    }

    private fun parseConnectionPoints(p2pXconnect: P2pXconnect, builder: ConnectionPointsBuilder) {
        val connectionPoint2 =
                p2pXconnect.pseudowires?.pseudowire?.getOrNull(0)
                        ?.let { parseRemoteEndpoint(it) }
                        ?: p2pXconnect.attachmentCircuits?.attachmentCircuit?.getOrNull(1)
                        ?.let { parseLocalConnectionPoint(it, CONNECTION_POINT_2) }

        val connectionPoint1 = p2pXconnect.attachmentCircuits?.attachmentCircuit?.getOrNull(0)
                ?.let { parseLocalConnectionPoint(it, CONNECTION_POINT_1) }

        if (connectionPoint1 == null || connectionPoint2 == null) {
            return
        }

        builder.connectionPoint = Lists.newArrayList(connectionPoint1, connectionPoint2)
    }

    private fun parseRemoteEndpoint(pseudoWire: Pseudowire): ConnectionPoint {

        val remoteSystem = IpAddress(pseudoWire.neighbor?.get(0)?.neighbor)
        val virtualCircuitId = pseudoWire.pseudowireId?.value

        val remote = RemoteBuilder()
                .setConfig(RemoteConfigBuilder()
                        .setRemoteSystem(remoteSystem)
                        .setVirtualCircuitIdentifier(virtualCircuitId)
                        .build())
                .build()

        val remoteEndpoint = EndpointBuilder()
                .setEndpointId(ENDPOINT_ID)
                .setConfig(EndpointConfigBuilder()
                        .setType(REMOTE::class.java)
                        .setEndpointId(ENDPOINT_ID)
                        .build())
                .setRemote(remote)
                .build()

        return createConnectionPoint(remoteEndpoint, CONNECTION_POINT_2)
    }

    private fun parseLocalConnectionPoint(attachementCircuit: AttachmentCircuit, connectionPointId: String):
        ConnectionPoint {

        val underlayIfcName = attachementCircuit.name?.value!!
        val local: Local

        if (Util.isSubinterface(underlayIfcName)) {
            local = LocalBuilder()
                    .setConfig(LocalConfigBuilder()
                            .setInterface(underlayIfcName.split(".").first())
                            .setSubinterface(underlayIfcName.split(".").last().toLong())
                            .build())
                    .build()
        } else {
            local = LocalBuilder()
                    .setConfig(LocalConfigBuilder()
                            .setInterface(underlayIfcName)
                            .build())
                    .build()
        }

        val localEndpoint = EndpointBuilder()
                .setEndpointId(ENDPOINT_ID)
                .setLocal(local)
                .setConfig(EndpointConfigBuilder()
                        .setEndpointId(ENDPOINT_ID)
                        .setType(LOCAL::class.java)
                        .build())
                .build()

        return createConnectionPoint(localEndpoint, connectionPointId)
    }

    private fun createConnectionPoint(endpoint: Endpoint, connectionPointId: String): ConnectionPoint {
        return ConnectionPointBuilder()
                .setConnectionPointId(connectionPointId)
                .setConfig(ConnectionPointConfigBuilder()
                        .setConnectionPointId(connectionPointId)
                        .build())
                .setEndpoints(EndpointsBuilder()
                        .setEndpoint(Collections.singletonList(endpoint))
                        .build())
                .build()
    }

    private fun isOper(ctx: ReadContext): Boolean {
        val flag = ctx.modificationCache.get(Reader.DS_TYPE_FLAG)
        return flag === LogicalDatastoreType.OPERATIONAL
    }

    companion object {
        val CONNECTION_POINT_1 = "1"
        val CONNECTION_POINT_2 = "2"

        val ENDPOINT_ID = "default"
    }
}