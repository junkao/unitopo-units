/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp.handler.table


import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.cli.registry.common.CompositeListReader
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.network.instance.common.L3VrfListReader
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalAfiSafiReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.TableConnection
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.TableConnectionBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.TableConnectionKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.table.connection.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.table.connection.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.ADDRESSFAMILY
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.IPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.IPV6
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.OSPF
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class BgpTableConnectionReader(private val access: UnderlayAccess) : L3VrfListReader.L3VrfConfigListReader<TableConnection, TableConnectionKey, TableConnectionBuilder>, CompositeListReader.Child<TableConnection, TableConnectionKey, TableConnectionBuilder> {

    override fun readCurrentAttributesForType(id: InstanceIdentifier<TableConnection>, b: TableConnectionBuilder, readContext: ReadContext) {
        val tcKey = id.firstKeyOf(TableConnection::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        val data = readData(access)

        parseRedistributes(data, vrfKey)
                .find { it.first == tcKey }
                ?.let {
                    b.addressFamily = it.first.addressFamily
                    b.srcProtocol = it.first.srcProtocol
                    b.dstProtocol = it.first.dstProtocol
                    b.config = it.second
                }
    }

    override fun getBuilder(id: InstanceIdentifier<TableConnection>) = TableConnectionBuilder()

    override fun getAllIdsForType(id: InstanceIdentifier<TableConnection>, readContext: ReadContext): List<TableConnectionKey> {
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        val data = readData(access)

        return parseRedistributes(data, vrfKey)
                .map { it.first }
                .distinct()
    }

    companion object {

        private fun readData(access: UnderlayAccess): Instance? {
            return access.read(BgpProtocolReader.UNDERLAY_BGP)
                    .checkedGet()
                    .orNull()
                    ?.instance.orEmpty()
                    .firstOrNull()
        }

        fun parseRedistributes(data: Instance?, vrfKey: NetworkInstanceKey): List<Pair<TableConnectionKey, Config>> {
            val fourByteAs = BgpProtocolReader.getFirst4ByteAs(data)

            val ospfRedis = if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
                GlobalAfiSafiReader.getGlobalAfs(fourByteAs)
                        .filter { it.ospfRoutes?.ospfRoute.orEmpty().isNotEmpty() }
                        .map { it.afName to it.ospfRoutes?.ospfRoute.orEmpty() }
            } else {
                GlobalAfiSafiReader.getVrfAfs(fourByteAs, vrfKey)
                        .filter { it.ospfRoutes?.ospfRoute.orEmpty().isNotEmpty() }
                        .map { it.afName to it.ospfRoutes?.ospfRoute.orEmpty() }
            }

            // Also parse config for the table connection, so that readCurrentAttributes can also reuse this
            return ospfRedis
                    .filter { it.first.toOpenconfig() != null }
                    .map {
                        TableConnectionKey(it.first.toOpenconfig(), BGP::class.java, OSPF::class.java) to
                                ConfigBuilder()
                                        .setAddressFamily(it.first.toOpenconfig())
                                        .setDstProtocol(BGP::class.java)
                                        .setSrcProtocol(OSPF::class.java)
                                        .setImportPolicy(it.second.mapNotNull { it.routePolicyName }.filter { it.isNotBlank() })
                                        .build()
                    }
        }
    }
}

fun BgpAddressFamily.toOpenconfig(): Class<out ADDRESSFAMILY>? {
    return when (this) {
        BgpAddressFamily.Ipv4Unicast -> IPV4::class.java
        BgpAddressFamily.Ipv6Unicast -> IPV6::class.java
        else -> null
    }
}