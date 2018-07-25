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
        val defaultAfIId = StaticRouteReader.ROUTE_STATIC_IID.child(DefaultVrf::class.java)
            .child(AddressFamily::class.java)!!
        val customAfIid = StaticRouteReader.ROUTE_STATIC_IID.child(Vrfs::class.java)
                .child(Vrf::class.java, VrfKey(CiscoIosXrString("Cust_A"))).child(AddressFamily::class.java)!!
    }
}