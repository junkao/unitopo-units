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

package io.frinx.unitopo.unit.xr7.network.instance.vrf.ifc

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces._interface.ConfigBuilder
import io.frinx.unitopo.unit.xr7.network.instance.vrf.VrfReaderTest as BaseTest

class VrfInterfaceConfigReaderTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var target: VrfInterfaceConfigReader

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        target = VrfInterfaceConfigReader()
    }

    companion object {
        private val IID_CONFIG = BaseTest.IID_NETWORK_INSTANCE
            .child(Interfaces::class.java)
            .child(Interface::class.java, InterfaceKey(BaseTest.BUN_ETH_301_1))
            .child(Config::class.java)
    }

    @Test
    fun testReadCurrentAttributes() {
        val builder = ConfigBuilder()
        target.readCurrentAttributes(IID_CONFIG, builder, readContext)
        Assert.assertEquals(BaseTest.BUN_ETH_301_1, builder.build().id)
    }

    @Test
    fun testGetBuilder() {
        val builder = target.getBuilder(IID_CONFIG)
        Assert.assertTrue(builder is ConfigBuilder)
    }

    @Test
    fun testMerge() {
        val builder = InterfaceBuilder()
        val config = ConfigBuilder().setId(BaseTest.BUN_ETH_301_1).build()
        target.merge(builder, config)
        Assert.assertSame(builder.config, config)
    }
}