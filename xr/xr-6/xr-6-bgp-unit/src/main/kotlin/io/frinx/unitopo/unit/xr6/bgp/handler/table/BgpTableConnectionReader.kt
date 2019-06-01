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

package io.frinx.unitopo.unit.xr6.bgp.handler.table

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.builder.BasicCheck
import io.fd.honeycomb.translate.spi.builder.Check
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
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

class BgpTableConnectionReader(private val access: UnderlayAccess) :
    CompositeListReader.Child<TableConnection, TableConnectionKey, TableConnectionBuilder> {

    override fun getCheck(): Check {
        return BasicCheck.emptyCheck()
    }

    override fun readCurrentAttributes(
        id: InstanceIdentifier<TableConnection>,
        b: TableConnectionBuilder,
        readContext: ReadContext
    ) {
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

    override fun getAllIds(id: InstanceIdentifier<TableConnection>, readContext: ReadContext):
        List<TableConnectionKey> {
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
                                        .setImportPolicy(it.second.mapNotNull { it.routePolicyName }.filter
                                        { it.isNotBlank() })
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