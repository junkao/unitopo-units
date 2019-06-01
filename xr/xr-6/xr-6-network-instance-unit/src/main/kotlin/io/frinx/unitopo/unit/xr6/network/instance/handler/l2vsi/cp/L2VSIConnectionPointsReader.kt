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

package io.frinx.unitopo.unit.xr6.network.instance.handler.l2vsi.cp

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.Reader
import io.fd.honeycomb.translate.spi.builder.BasicCheck
import io.fd.honeycomb.translate.spi.builder.Check
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.network.instance.handler.l2vsi.L2VSIReader
import io.frinx.unitopo.unit.xr6.network.instance.handler.l2vsi.cp.L2VSIConnectionPointsReader.Companion.ENDPOINT_ID
import io.frinx.unitopo.unit.xr6.network.instance.handler.l2vsi.cp.L2VSIConnectionPointsReader.Companion.REMOTE_ID
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.BridgeDomain
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.vfis.Vfi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPoints
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPointsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.ConnectionPoint
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.ConnectionPointBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.ConnectionPointKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.EndpointsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.EndpointBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.endpoint.LocalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.endpoint.RemoteBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.LOCAL
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.REMOTE
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.ConfigBuilder as CpConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.endpoint.ConfigBuilder as EpConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.endpoint.local.ConfigBuilder as LocalConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.endpoint.remote.ConfigBuilder as RemoteConfigBuilder

class L2VSIConnectionPointsReader(private val underlayAccess: UnderlayAccess) :
    CompositeReader.Child<ConnectionPoints, ConnectionPointsBuilder> {

    override fun getCheck(): Check {
        return BasicCheck.checkData(ChecksMap.DataCheck.NetworkInstanceConfig.IID_TRANSFORMATION,
            ChecksMap.DataCheck.NetworkInstanceConfig.TYPE_L2VSI)
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
        val l2vsiName = id.firstKeyOf(NetworkInstance::class.java).name
        val isOper = isOper(ctx)

        val bd = L2VSIReader.getAllBridgeDomains(underlayAccess)
            .firstOrNull { bd -> bd.name.value == l2vsiName }

        if (bd == null) return

        bd.vfis?.vfi.orEmpty()
            .firstOrNull { vfi -> vfi.name.value == l2vsiName }
            ?.let { vfi -> builder.fromUnderlay(bd, vfi, isOper) }
    }

    private fun isOper(ctx: ReadContext) =
        ctx.modificationCache.get(Reader.DS_TYPE_FLAG) === LogicalDatastoreType.OPERATIONAL

    companion object {
        val REMOTE_ID = "remote"
        val ENDPOINT_ID = "default"
        val VE_ID = 1L
    }
}

private fun ConnectionPointsBuilder.fromUnderlay(bd: BridgeDomain, vfi: Vfi, isOper: Boolean) {

    val vccId = vfi.vpnid?.value
    val routeTarget = vfi.bgpAutoDiscovery?.routeTargets?.routeTarget.orEmpty()
        .first()?.twoByteAsOrFourByteAs.orEmpty().first()

    // Looking for vpn with vccId == routeTarget.asIndex.
    if (vccId == null) return
    if (routeTarget == null) return
    if (routeTarget.`as` == null) return
    if (routeTarget.`asIndex` == null) return
    if (routeTarget.asIndex.value != vccId) return

    val connectionPoints = mutableListOf<ConnectionPoint>()
    connectionPoints.add(parseRemotePoint(vccId, isOper))
    connectionPoints.addAll(parseLocalPoints(bd, isOper))

    connectionPoint = connectionPoints
}

fun parseLocalPoints(bd: BridgeDomain, isOper: Boolean): List<ConnectionPoint> {
    // FIXME finish isOper

    return bd.bdAttachmentCircuits?.bdAttachmentCircuit.orEmpty()
        .map { atCirc -> atCirc.name.value }
        .map { ifcName -> toLocalPoint(ifcName) }
}

fun toLocalPoint(ifcName: String): ConnectionPoint {
    val connectionPointBuilder = ConnectionPointBuilder()

    connectionPointBuilder
        .setKey(ConnectionPointKey(ifcName))
        .config = CpConfigBuilder()
        .setConnectionPointId(ifcName)
        .build()

    val localCfgBuilder = LocalConfigBuilder()
        .setInterface(ifcName)

    if (ifcName.contains('.')) {
        val split = ifcName.split('.')
        localCfgBuilder.`interface` = split[0]
        localCfgBuilder.subinterface = split[1].toLong()
    }

    connectionPointBuilder.endpoints = EndpointsBuilder()
        .setEndpoint(
            listOf(
                EndpointBuilder()
                    .setEndpointId(ifcName)
                    .setConfig(
                        EpConfigBuilder()
                            .setEndpointId(ifcName)
                            .setPrecedence(0)
                            .setType(LOCAL::class.java)
                            .build()
                    )
                    .setLocal(
                        LocalBuilder()
                            .setConfig(
                                localCfgBuilder
                                    .build()
                            )
                            .build()
                    ).build()
            )
        )
        .build()

    return connectionPointBuilder.build()
}

fun parseRemotePoint(vccid: Long, isOper: Boolean): ConnectionPoint {
    val connectionPointBuilder = ConnectionPointBuilder()

    // FIXME finish isOper

    connectionPointBuilder
        .setKey(ConnectionPointKey(REMOTE_ID))
        .config = CpConfigBuilder()
        .setConnectionPointId(REMOTE_ID)
        .build()

    connectionPointBuilder.endpoints = EndpointsBuilder()
        .setEndpoint(
            listOf(
                EndpointBuilder()
                    .setEndpointId(ENDPOINT_ID)
                    .setConfig(
                        EpConfigBuilder()
                            .setEndpointId(ENDPOINT_ID)
                            .setPrecedence(0)
                            .setType(REMOTE::class.java)
                            .build()
                    )
                    .setRemote(
                        RemoteBuilder()
                            .setConfig(
                                RemoteConfigBuilder()
                                    .setVirtualCircuitIdentifier(vccid)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
        )
        .build()

    return connectionPointBuilder.build()
}