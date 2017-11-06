/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev170202.bgp.global.base.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev170202.bgp.global.base.StateBuilder
import io.frinx.unitopo.unit.xr6.bgp.handler.fromUnderlay
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString


class GlobalReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/bgp-oper.xml")

    @Test
    fun testGlobal() {
        val cBuilder = ConfigBuilder()
        cBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES, BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))), "default")

        val sBuilder = StateBuilder()
        sBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES, BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java,InstanceKey(CiscoIosXrString("default")))), "default")

        Assert.assertEquals(65000, cBuilder.`as`.value)
        Assert.assertEquals(65000, sBuilder.`as`.value)

        Assert.assertEquals("10.0.0.1", cBuilder.routerId.value)
        Assert.assertEquals("10.0.0.1", sBuilder.routerId.value)
    }
}