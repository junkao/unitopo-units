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
import io.frinx.unitopo.unit.xr6.lr.handler.InterfaceConfigReader
import io.frinx.unitopo.unit.xr6.lr.handler.NextHopReader
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.StaticKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHopKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.ref._interface.ref.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpPrefix
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv4Prefix

class InterfaceConfigReaderTest : AbstractNetconfHandlerTest() {

    private val staticData = getResourceAsString("/static_routes.xml")

    @Test
    fun testInterfaceConfig() {
        val defaultFamily = parseGetCfgResponse(staticData, StaticRouteReaderTest.defaultAfIId)
        val table = NextHopReader.parseNextHopTable(defaultFamily, StaticKey(IpPrefix(Ipv4Prefix("1.1.1.1/32"))))
        Assert.assertNotNull(table)

        val builder = ConfigBuilder()

        InterfaceConfigReader.parseInterface(table!!, builder, NextHopKey("10.1.1.1 GigabitEthernet0/0/0/1"))
        Assert.assertEquals("GigabitEthernet0/0/0/1", builder.`interface`)
    }
}