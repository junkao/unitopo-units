/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp.handler

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP


class GlobalReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/bgp-conf.xml")
    private val DATA_NODES2 = getResourceAsString("/bgp-conf2.xml")

    @Test
    fun testGlobal() {
        val ids = BgpProtocolReader.parseIds(parseGetCfgResponse(DATA_NODES, BgpProtocolReader.UNDERLAY_BGP))
        Assert.assertEquals(listOf(ProtocolKey(BGP::class.java,"default")), ids)

        val cBuilder = ConfigBuilder()
        cBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))), "default")

        Assert.assertEquals(65000, cBuilder.`as`.value)
        Assert.assertEquals("10.0.0.1", cBuilder.routerId.value)
    }

    @Test
    fun testGlobal2() {
        val ids = BgpProtocolReader.parseIds(parseGetCfgResponse(DATA_NODES, BgpProtocolReader.UNDERLAY_BGP))
        Assert.assertEquals(listOf(ProtocolKey(BGP::class.java,"default")), ids)

        val cBuilder = ConfigBuilder()
        cBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES2,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))), "default")

        Assert.assertEquals(720919, cBuilder.`as`.value)
        Assert.assertEquals("1.1.1.1", cBuilder.routerId.value)
    }

    @Test
    fun testGlobal2Vrf() {
        val cBuilder = ConfigBuilder()
        cBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES2,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))), "abcd")

        Assert.assertEquals(720919, cBuilder.`as`.value)


        val cBuilder2 = ConfigBuilder()
        val cBuilderVerify = ConfigBuilder()
        cBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES2,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))), "NOTEXISTING")

        Assert.assertEquals(cBuilderVerify.build(), cBuilder2.build())
    }

}