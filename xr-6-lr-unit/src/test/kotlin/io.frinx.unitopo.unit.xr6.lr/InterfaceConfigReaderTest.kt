/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lr

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.xr6.lr.handler.InterfaceConfigReader
import io.frinx.unitopo.unit.xr6.lr.handler.NextHopReader
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.StaticKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHopKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222._interface.ref._interface.ref.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.types.inet.rev170403.IpPrefix
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.types.inet.rev170403.Ipv4Prefix

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