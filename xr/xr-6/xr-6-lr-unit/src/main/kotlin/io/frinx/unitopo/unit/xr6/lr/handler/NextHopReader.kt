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

package io.frinx.unitopo.unit.xr6.lr.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.address.family.AddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.next.hop.VRFNEXTHOPCONTENT
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.prefix.table.VrfPrefixes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.route.vrf.route.VrfNextHopTable
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.Static
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.StaticKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.NextHopsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHop
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHopBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHopKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.next.hop.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.next.hop.StateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv4Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv6Address
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.ArrayList
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.LocalStaticNexthopConfig.NextHop as BASE_NEXTHOP_CONFIG

class NextHopReader(private val access: UnderlayAccess) :
    ConfigListReaderCustomizer<NextHop, NextHopKey, NextHopBuilder> {

    override fun getBuilder(id: InstanceIdentifier<NextHop>) = NextHopBuilder()

    override fun readCurrentAttributes(
        id: InstanceIdentifier<NextHop>,
        builder: NextHopBuilder,
        ctx: ReadContext
    ) {
        if (access.currentOperationType == LogicalDatastoreType.CONFIGURATION) {
            // FIXME Since this mixes config and oper data, it can only work in oper reads
            return
        }

        val key = id.firstKeyOf(NextHop::class.java)
        builder.index = key.index
        parseNextHopTable(access, id)?.let {
            parseNextHopContent(key, builder, it)
        }
    }

    override fun getAllIds(id: InstanceIdentifier<NextHop>, context: ReadContext): List<NextHopKey> {
        if (access.currentOperationType == LogicalDatastoreType.CONFIGURATION) {
            // FIXME Since this mixes config and oper data, it can only work in oper reads
            return emptyList()
        }

        parseNextHopTable(access, id)?.let {
            return getKeys(it)
        }
        return emptyList()
    }

    override fun merge(builder: Builder<out DataObject>, readData: List<NextHop>) {
        (builder as NextHopsBuilder).nextHop = readData
    }

    companion object {

        private fun parseNextHopTable(access: UnderlayAccess, id: InstanceIdentifier<NextHop>): VrfNextHopTable? {
            val routeKey = id.firstKeyOf(Static::class.java)
            val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
            val af = StaticRouteReader.getAddressFamily(access, vrfName)
            af?.let {
                return parseNextHopTable(af, routeKey)
            }
            return null
        }

        @VisibleForTesting
        fun parseNextHopTable(af: AddressFamily, routeKey: StaticKey): VrfNextHopTable? {

            // prefix can't be in unicast at the same time as in multicast, so if we didn't find it in unicast, let's check multicast
            routeKey.prefix?.ipv4Prefix?.value?.let {
                af.vrfipv4!!.vrfUnicast?.vrfPrefixes?.findPrefix(routeKey)?.let {
                    return it.vrfRoute?.vrfNextHopTable
                }
                af.vrfipv4!!.vrfMulticast?.vrfPrefixes?.findPrefix(routeKey)?.let {
                    return it.vrfRoute?.vrfNextHopTable
                }
            }

            routeKey.prefix?.ipv6Prefix?.value?.let {
                af.vrfipv6!!.vrfUnicast?.vrfPrefixes?.findPrefix(routeKey)?.let {
                    return it.vrfRoute?.vrfNextHopTable
                }
                af.vrfipv6!!.vrfMulticast?.vrfPrefixes?.findPrefix(routeKey)?.let {
                    return it.vrfRoute?.vrfNextHopTable
                }
            }
            return null
        }

        @VisibleForTesting
        fun getKeys(table: VrfNextHopTable): List<NextHopKey> {
            val keys = ArrayList<NextHopKey>()
            // only interface
            table.vrfNextHopInterfaceName.orEmpty().forEach { keys.add(NextHopKey(it.interfaceName.value)) }

            // interface + nexthop
            table.vrfNextHopInterfaceNameNextHopAddress.orEmpty().forEach { keys.add(it.nextHopAddress
                .createComplexKey(it.interfaceName.value)) }

            // only next hop
            table.vrfNextHopNextHopAddress.orEmpty().forEach { keys.add(it.nextHopAddress.createComplexKey(null)) }
            return keys
        }

        @VisibleForTesting
        fun parseNextHopContent(key: NextHopKey, builder: NextHopBuilder, table: VrfNextHopTable) {
            val cBuilder = ConfigBuilder()
            val sBuilder = StateBuilder()
            // only next hop
            table.vrfNextHopNextHopAddress.orEmpty()
                    .firstOrNull { it.nextHopAddress.createComplexKey(null) == key }?.let {
                cBuilder.nextHop = BASE_NEXTHOP_CONFIG(ipFromIpAddressNoZone(it.nextHopAddress))
                sBuilder.nextHop = BASE_NEXTHOP_CONFIG(ipFromIpAddressNoZone(it.nextHopAddress))
                setMetric(cBuilder, sBuilder, it)
            }

            // only interface
            table.vrfNextHopInterfaceName.orEmpty()
                    .firstOrNull { it.interfaceName.value == key.toString() }?.let {
                setMetric(cBuilder, sBuilder, it)
            }

            // interface + nexthop
            table.vrfNextHopInterfaceNameNextHopAddress.orEmpty()
                    .firstOrNull { it.nextHopAddress.createComplexKey(it.interfaceName.value) == key }?.let {
                cBuilder.nextHop = BASE_NEXTHOP_CONFIG(ipFromIpAddressNoZone(it.nextHopAddress))
                sBuilder.nextHop = BASE_NEXTHOP_CONFIG(ipFromIpAddressNoZone(it.nextHopAddress))
                setMetric(cBuilder, sBuilder, it)
            }
            cBuilder.index = key.index
            sBuilder.index = key.index

            builder.config = cBuilder.build()
            builder.state = sBuilder.build()
        }

        private fun setMetric(cBuilder: ConfigBuilder, sBuilder: StateBuilder, content: VRFNEXTHOPCONTENT) {
            cBuilder.metric = content.loadMetric
            sBuilder.metric = content.loadMetric
        }

        private fun VrfPrefixes.findPrefix(routeKey: StaticKey) = vrfPrefix.orEmpty().firstOrNull { it
            .ipAddressToPrefix().ipv4Prefix == routeKey.prefix.ipv4Prefix }

        private fun ipFromIpAddressNoZone(ipNoZone: IpAddressNoZone): IpAddress {
            ipNoZone.ipv4AddressNoZone?.let {
                return IpAddress(Ipv4Address(ipNoZone.ipv4AddressNoZone.value))
            }
            return IpAddress(Ipv6Address(ipNoZone.ipv6AddressNoZone.value))
        }
    }
}

fun IpAddressNoZone.createComplexKey(interfaceName: String?): NextHopKey {
    val builder = StringBuilder()
    builder.append(ipv4AddressNoZone?.value ?: ipv6AddressNoZone?.value)
    interfaceName?.let {
        builder.append(" ").append(interfaceName)
    }
    return NextHopKey(builder.toString())
}