/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lr.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.lr.common.LrListReader
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMMountPointServiceAdapter.LOG
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.next.hop.VRFNEXTHOPCONTENT
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.route.vrf.route.VrfNextHopTable
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.Static
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.NextHopsBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHop
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHopBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHopKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.next.hop.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.next.hop.StateBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.types.inet.rev170403.Ipv4Address
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.types.inet.rev170403.Ipv6Address
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.*

class NextHopReader(private val access: UnderlayAccess) : LrListReader<NextHop, NextHopKey, NextHopBuilder> {

    override fun getBuilder(id: InstanceIdentifier<NextHop>): NextHopBuilder = NextHopBuilder()

    override fun readCurrentAttributesForType(id: InstanceIdentifier<NextHop>, builder: NextHopBuilder, ctx: ReadContext) {
        val key = id.firstKeyOf(NextHop::class.java)
        builder.index = key.index

        val table = parseNextHopTable(access, id)

        val cBuilder = ConfigBuilder()
        val sBuilder = StateBuilder()
        cBuilder.index = key.index
        sBuilder.index = key.index

        var sureContent : VRFNEXTHOPCONTENT? = null
        val maybeContent1 = table?.vrfNextHopNextHopAddress.orEmpty().stream()
                .filter { f -> createComplexKey(null, f.nextHopAddress) == key }.findFirst()
        if (maybeContent1.isPresent) {
            sureContent = maybeContent1.get()
            cBuilder.nextHop = org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.LocalStaticNexthopConfig.NextHop(ipFromIpAddressNoZone(maybeContent1.get().nextHopAddress))
            sBuilder.nextHop = org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.LocalStaticNexthopConfig.NextHop(ipFromIpAddressNoZone(maybeContent1.get().nextHopAddress))
        } else {
            val maybeContent2 = table?.vrfNextHopInterfaceName.orEmpty().stream().filter { f -> f.interfaceName.value == key.toString() }.findFirst()
            if (maybeContent2.isPresent) {
                sureContent = maybeContent2.get()
            } else {
                val maybeContent3 = table?.vrfNextHopInterfaceNameNextHopAddress.orEmpty().stream()
                        .filter { f -> createComplexKey(f.interfaceName.value, f.nextHopAddress) == key }.findFirst()
                if (maybeContent3.isPresent) {
                    sureContent = maybeContent3.get()
                    cBuilder.nextHop = org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.LocalStaticNexthopConfig.NextHop(ipFromIpAddressNoZone(maybeContent3.get().nextHopAddress))
                    sBuilder.nextHop = org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.LocalStaticNexthopConfig.NextHop(ipFromIpAddressNoZone(maybeContent3.get().nextHopAddress))
                }
            }
        }
        cBuilder.metric = sureContent?.loadMetric
        sBuilder.metric = sureContent?.loadMetric

        builder.config = cBuilder.build()
        builder.state = sBuilder.build()
    }

    private fun ipFromIpAddressNoZone(ipNoZone : IpAddressNoZone) : IpAddress {
        return if (ipNoZone.ipv4AddressNoZone != null) {
            IpAddress(Ipv4Address(ipNoZone.ipv4AddressNoZone.value))
        } else {
            IpAddress(Ipv6Address(ipNoZone.ipv6AddressNoZone.value))
        }
    }

    @Throws(ReadFailedException::class)
    override fun getAllIdsForType(id: InstanceIdentifier<NextHop>, context: ReadContext): List<NextHopKey> {
        val table = parseNextHopTable(access, id)

        val keys = ArrayList<NextHopKey>()
        // only interface
        table?.vrfNextHopInterfaceName?.stream()?.forEach { name -> keys.add(NextHopKey(name.interfaceName.value)) }

        // interface + nexthop
        table?.vrfNextHopInterfaceNameNextHopAddress?.stream()?.forEach { name -> keys.add(createComplexKey(name.interfaceName.value, name.nextHopAddress)) }

        // only next hop
        table?.vrfNextHopNextHopAddress?.stream()?.forEach { name -> keys.add(createComplexKey(null, name.nextHopAddress)) }
        return keys
    }

    override fun merge(builder: Builder<out DataObject>, readData: List<NextHop>) {
        (builder as NextHopsBuilder).nextHop = readData
    }

    companion object {

        fun createComplexKey(interfaceName: String?, nextHop: IpAddressNoZone): NextHopKey {
            val builder = StringBuilder()
            builder.append(nextHop.ipv4AddressNoZone?.value ?: nextHop.ipv6AddressNoZone?.value)
            if (interfaceName != null) {
                builder.append(" ").append(interfaceName)
            } else {
                builder.append("")
            }
            return NextHopKey(builder.toString())
        }

        fun parseNextHopTable(access: UnderlayAccess, id: InstanceIdentifier<NextHop>): VrfNextHopTable? {
            val routeKey = id.firstKeyOf(Static::class.java)
            val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
            val af = StaticRouteReader.getAddressFamily(access, vrfName)
            af?: return null

            if (routeKey.prefix?.ipv4Prefix?.value != null) {
                LOG.debug("ipv4: {}", routeKey.prefix.ipv4Prefix.value)
                // prefix can't be in unicast at the same time as in multicast, so if we didn't find it in unicast, let's check multicast
                var maybePref = af.vrfipv4!!.vrfUnicast?.vrfPrefixes?.vrfPrefix?.stream()
                        ?.filter { vrf -> StaticRouteReader.ipAddressToPrefix(vrf).ipv4Prefix == routeKey.prefix.ipv4Prefix }?.findFirst()!!
                if (!maybePref.isPresent) {
                    maybePref = af.vrfipv4!!.vrfMulticast?.vrfPrefixes?.vrfPrefix?.stream()
                            ?.filter { vrf ->  StaticRouteReader.ipAddressToPrefix(vrf).ipv4Prefix == routeKey.prefix.ipv4Prefix }?.findFirst()!!
                }
                return maybePref.get().vrfRoute.vrfNextHopTable
            }

            if (routeKey.prefix?.ipv6Prefix?.value != null) {
                LOG.debug("ipv6: {}", routeKey.prefix.ipv6Prefix.value)
                // prefix can't be in unicast at the same time as in multicast, so if we didn't find it in unicast, let's check multicast
                var maybePref = af.vrfipv6?.vrfUnicast?.vrfPrefixes?.vrfPrefix?.stream()
                        ?.filter { vrf -> StaticRouteReader.ipAddressToPrefix(vrf).ipv6Prefix == routeKey.prefix.ipv6Prefix }?.findFirst()!!
                if (!maybePref.isPresent) {
                    maybePref = af.vrfipv6?.vrfMulticast?.vrfPrefixes?.vrfPrefix?.stream()
                            ?.filter { vrf -> StaticRouteReader.ipAddressToPrefix(vrf).ipv6Prefix == routeKey.prefix.ipv6Prefix }?.findFirst()!!
                }
                return maybePref.get().vrfRoute.vrfNextHopTable
            }
            return null
        }
    }
}
