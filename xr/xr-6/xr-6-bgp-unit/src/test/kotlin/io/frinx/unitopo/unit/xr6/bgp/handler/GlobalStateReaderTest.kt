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

import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.StateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP

class GlobalStateReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/bgp-default-oper.xml")
    private val DATA_NODES2 = getResourceAsString("/bgp-vrf-oper.xml")

    @Test
    fun testGlobal() {
        val sBuilder = StateBuilder()
        sBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES,
                GlobalStateReader.getId(ProtocolKey(BGP::class.java, "default"), NetworInstance.DEFAULT_NETWORK)))

        Assert.assertEquals(720919, sBuilder.`as`.value)
        Assert.assertEquals("1.1.1.1", sBuilder.routerId.value)
    }

    @Test
    fun testGlobalVrf() {
        val sBuilder = StateBuilder()
        sBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES2,
                GlobalStateReader.getId(ProtocolKey(BGP::class.java, "default"), NetworkInstanceKey("abcd"))))

        Assert.assertEquals(720919, sBuilder.`as`.value)
        Assert.assertEquals("8.8.8.8", sBuilder.routerId.value)
    }
}