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
import io.frinx.unitopo.unit.junos18.network.instance.handler.vrf.L3VrfReader
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey

class VrfReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private val target = L3VrfReader(underlayAccess)

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testReadCurrentAttributes() {
        val ifName = "THU"
        val id = IIDs.NETWORKINSTANCES.child(NetworkInstance::class.java, NetworkInstanceKey(ifName))
        val builder = NetworkInstanceBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.name, CoreMatchers.sameInstance(ifName))
    }

    @Test
    fun testGetAllIds() {
        val id = IIDs.NE_NETWORKINSTANCE

        val result = target.getAllIds(id, readContext)

        Assert.assertThat(
                result.map { it.name },
                Matchers.containsInAnyOrder("MS-THU", "THU")
        )
    }
}