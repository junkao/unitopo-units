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

package io.frinx.unitopo.unit.xr6.ospf.handler.table

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.handlers.l3vrf.L3VrfListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.ospf.handler.OspfProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.OspfRedistProtocol
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.Processes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.redistribution.redistributes.Redistribute
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.TableConnection
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.TableConnectionBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.TableConnectionKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.table.connection.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.table.connection.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.IPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.INSTALLPROTOCOLTYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.OSPF
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class OspfTableConnectionReader(private val access: UnderlayAccess)
    : L3VrfListReader.L3VrfConfigListReader<TableConnection, TableConnectionKey, TableConnectionBuilder>,
    CompositeListReader.Child<TableConnection, TableConnectionKey, TableConnectionBuilder> {
    override fun readCurrentAttributesForType(
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

    override fun getAllIdsForType(
        id: InstanceIdentifier<TableConnection>,
        readContext: ReadContext
    ): List<TableConnectionKey> {
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        val data = readData(access)

        return parseRedistributes(data, vrfKey)
                .map { it.first }
                .distinct()
    }

    companion object {

        private fun readData(access: UnderlayAccess): Processes? {
            return access.read(OspfProtocolReader.UNDERLAY_OSPF)
                    .checkedGet()
                    .orNull()
        }

        fun parseRedistributes(data: Processes?, vrfKey: NetworkInstanceKey): List<Pair<TableConnectionKey, Config>> {
            val redis = if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
                data?.process.orEmpty()
                        .mapNotNull { it.defaultVrf }
                        .mapNotNull { it.redistribution }
                        .mapNotNull { it.redistributes }
                        .flatMap { it.redistribute.orEmpty() }
                        .map { it.protocolName to it }
            } else {
                data?.process.orEmpty()
                        .mapNotNull { it.vrfs }
                        .mapNotNull { it.vrf.orEmpty().find { it.vrfName.value == vrfKey.name } }
                        .mapNotNull { it.redistribution }
                        .mapNotNull { it.redistributes }
                        .flatMap { it.redistribute.orEmpty() }
                        .map { it.protocolName to it }
            }

            // Also parse config for the table connection, so that readCurrentAttributes can also reuse this
            return redis
                    .filter { it.first.toOpenconfig() != null }
                    .map {
                        TableConnectionKey(IPV4::class.java, OSPF::class.java, it.first.toOpenconfig()) to
                                ConfigBuilder()
                                        .setAddressFamily(IPV4::class.java)
                                        .setDstProtocol(OSPF::class.java)
                                        .setSrcProtocol(it.first.toOpenconfig())
                                        .setImportPolicy(it.second.getAllRoutePolicies(it.first.toOpenconfig()))
                                        .build()
                    }
        }
    }
}

private fun OspfRedistProtocol.toOpenconfig(): Class<out INSTALLPROTOCOLTYPE>? {
    return when (this) {
        OspfRedistProtocol.Ospf -> OSPF::class.java
        OspfRedistProtocol.Bgp -> BGP::class.java
        else -> null
    }
}

private fun Redistribute.getAllRoutePolicies(srcProtocol: Class<out INSTALLPROTOCOLTYPE>?): List<String> {
    return when (srcProtocol) {
        BGP::class.java -> this.bgp.orEmpty().mapNotNull { it.routePolicyName }
        OSPF::class.java -> this.applicationOrIsisOrOspf.orEmpty().mapNotNull { it.routePolicyName }
        else -> emptyList()
    }.filter { it.isNotBlank() }
}