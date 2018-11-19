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

package io.frinx.unitopo.unit.junos18.network.instance.vrf

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L3VRF
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.NETWORKINSTANCETYPE

class VrfConfigReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private val target = VrfConfigReader(underlayAccess)

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testReadCurrentAttributes() {
        val ifName = "THU"
        val id = IIDs.NETWORKINSTANCES
                .child(NetworkInstance::class.java, NetworkInstanceKey(ifName))
                .child(Config::class.java)
        val builder = ConfigBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.name, CoreMatchers.sameInstance(ifName))
        Assert.assertThat(
                builder.type,
                CoreMatchers.equalTo(L3VRF::class.java) as Matcher<Class<out NETWORKINSTANCETYPE>>
        )
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testMerge() {
        val parentBuilder = NetworkInstanceBuilder()
        val data: Config = Mockito.mock(Config::class.java)

        target.merge(parentBuilder, data)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testGetBuilder() {
        target.getBuilder(IIDs.NE_NE_CONFIG)
    }
}