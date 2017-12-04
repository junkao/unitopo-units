/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lr

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.xr6.lr.handler.StaticRouteReader
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.address.family.AddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.router._static.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.router._static.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.router._static.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.router._static.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.StaticKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpPrefix
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv4Prefix
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv6Prefix

class StaticRouteReaderTest : AbstractNetconfHandlerTest() {

    private val staticData = getResourceAsString("/static_routes.xml")

    @Test
    fun testAllIds() {
        // default
        val defaultFamily = parseGetCfgResponse(staticData, defaultAfIId)

        Assert.assertEquals(
                listOf(IpPrefix(Ipv4Prefix("1.1.1.1/32")), IpPrefix(Ipv6Prefix("2001:1:1:1::/64")))
                        .map { StaticKey(it) }
                        .toSet(),
                StaticRouteReader.getStaticKeys(defaultFamily).toSet())


        // vrf Cust_A
        val customFamily = parseGetCfgResponse(staticData, customAfIid)

        Assert.assertEquals(
                listOf(IpPrefix(Ipv4Prefix("1.1.1.1/32")))
                        .map { StaticKey(it) }
                        .toSet(),
                StaticRouteReader.getStaticKeys(customFamily).toSet())
    }

    companion object {
        val defaultAfIId = StaticRouteReader.ROUTE_STATIC_IID.child(DefaultVrf::class.java).child(AddressFamily::class.java)!!
        val customAfIid = StaticRouteReader.ROUTE_STATIC_IID.child(Vrfs::class.java)
                .child(Vrf::class.java, VrfKey(CiscoIosXrString("Cust_A"))).child(AddressFamily::class.java)!!
    }
}