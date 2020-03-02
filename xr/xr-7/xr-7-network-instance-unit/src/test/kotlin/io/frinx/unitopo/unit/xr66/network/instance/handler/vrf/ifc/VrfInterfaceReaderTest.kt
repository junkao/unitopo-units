/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.network.instance.handler.vrf.ifc

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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import io.frinx.unitopo.unit.xr7.network.instance.handler.vrf.L3VrfReaderTest as BaseTest

class VrfInterfaceReaderTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: VrfInterfaceReader

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(BaseTest.NC_HELPER))
        target = VrfInterfaceReader(underlayAccess)
    }

    companion object {
        private val IID_INTERFACE = InstanceIdentifier
            .create(NetworkInstances::class.java)
            .child(NetworkInstance::class.java, NetworkInstanceKey(BaseTest.VRF_IM1))
            .child(Interfaces::class.java)
            .child(Interface::class.java, InterfaceKey(BaseTest.BUN_ETH_301_1))
    }

    @Test
    fun testReadCurrentAttributesForType() {
        val builder = InterfaceBuilder()
        target.readCurrentAttributes(IID_INTERFACE, builder, readContext)
        Assert.assertEquals(BaseTest.BUN_ETH_301_1, builder.build().id)
    }

    @Test
    fun testGetAllIdsForType() {
        val list = target.getAllIds(IID_INTERFACE, readContext)
        Assert.assertThat(
            list.map { it.id },
            Matchers.containsInAnyOrder(BaseTest.BUN_ETH_301_1)
        )
    }
}