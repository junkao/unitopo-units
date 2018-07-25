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
        Assert.assertEquals(listOf(ProtocolKey(BGP::class.java, "default")), ids)

        val cBuilder = ConfigBuilder()
        cBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))),
            "default")

        Assert.assertEquals(65000, cBuilder.`as`.value)
        Assert.assertEquals("10.0.0.1", cBuilder.routerId.value)
    }

    @Test
    fun testGlobal2() {
        val ids = BgpProtocolReader.parseIds(parseGetCfgResponse(DATA_NODES, BgpProtocolReader.UNDERLAY_BGP))
        Assert.assertEquals(listOf(ProtocolKey(BGP::class.java, "default")), ids)

        val cBuilder = ConfigBuilder()
        cBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES2,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))),
            "default")

        Assert.assertEquals(720919, cBuilder.`as`.value)
        Assert.assertEquals("1.1.1.1", cBuilder.routerId.value)
    }

    @Test
    fun testGlobal2Vrf() {
        val cBuilder = ConfigBuilder()
        cBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES2,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))),
            "abcd")

        Assert.assertEquals(720919, cBuilder.`as`.value)

        val cBuilder2 = ConfigBuilder()
        val cBuilderVerify = ConfigBuilder()
        cBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES2,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))),
            "NOTEXISTING")

        Assert.assertEquals(cBuilderVerify.build(), cBuilder2.build())
    }
}