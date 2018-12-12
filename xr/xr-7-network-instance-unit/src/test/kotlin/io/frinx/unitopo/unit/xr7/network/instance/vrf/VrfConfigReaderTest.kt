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

package io.frinx.unitopo.unit.xr7.network.instance.vrf

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L3VRF

class VrfConfigReaderTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var target: VrfConfigReader

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        target = VrfConfigReader()
    }

    companion object {
        private val IID_CONFIG = VrfReaderTest.IID_NETWORK_INSTANCE
            .child(Config::class.java)
    }

    @Test
    fun testReadCurrentAttributes() {
        val builder = ConfigBuilder()
        target.readCurrentAttributes(IID_CONFIG, builder, readContext)
        Assert.assertEquals(VrfReaderTest.VRF_IM1, builder.build().name)
        Assert.assertEquals(L3VRF::class.java, builder.build().type)
    }

    @Test
    fun testMerge() {
        val builder = NetworkInstanceBuilder()
        val config = ConfigBuilder().apply {
            this.name = VrfReaderTest.VRF_IM1
            this.type = L3VRF::class.java
        }.build()

        target.merge(builder, config)
        Assert.assertSame(builder.config, config)
    }
}