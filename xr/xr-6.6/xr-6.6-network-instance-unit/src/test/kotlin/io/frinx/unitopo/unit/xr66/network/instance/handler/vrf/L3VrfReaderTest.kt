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

package io.frinx.unitopo.unit.xr66.network.instance.handler.vrf

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class L3VrfReaderTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: L3VrfReader

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = L3VrfReader(underlayAccess)
    }

    companion object {
        val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        val BUN_ETH_301_1 = "Bundle-Ether301.1"
        val BUN_ETH_301_2 = "Bundle-Ether301.2"
        val VRF_IM1 = "imm"
        val VRF_IM2 = "mmi"
        val IID_NETWORK_INSTANCE = InstanceIdentifier
            .create(NetworkInstances::class.java)
            .child(NetworkInstance::class.java, NetworkInstanceKey("imm"))
    }

    @Test
    fun testReadCurrentAttributes() {
        val builder = NetworkInstanceBuilder()
        target.readCurrentAttributes(IID_NETWORK_INSTANCE, builder, readContext)
        Assert.assertEquals(VRF_IM1, builder.build().name)
    }

    @Test
    fun testGetAllIds() {
        val list = target.getAllIds(IID_NETWORK_INSTANCE, readContext)
        // the list should contains iids from interface,bgp and ospf block
        Assert.assertThat(
            list.map { it.name },
            Matchers.containsInAnyOrder(
                "default",
                VRF_IM1,
                VRF_IM2,
                "THU"
            )
        )
    }
}