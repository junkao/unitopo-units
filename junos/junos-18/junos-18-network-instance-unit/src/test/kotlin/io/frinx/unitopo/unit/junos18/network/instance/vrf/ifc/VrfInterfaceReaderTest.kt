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

package io.frinx.unitopo.unit.junos18.network.instance.vrf.ifc

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.network.instance.handler.vrf.ifc.VrfInterfaceReader
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.InterfaceKey

class VrfInterfaceReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private val target = VrfInterfaceReader(underlayAccess)

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testGetAllIdsForType() {
        val vrfName = "MS-THU"
        val id = IIDs.NETWORKINSTANCES
                .child(NetworkInstance::class.java, NetworkInstanceKey(vrfName))
                .child(Interfaces::class.java)
                .child(Interface::class.java)

        val result = target.getAllIds(id, readContext)
        Assert.assertThat(
                result.map { it.id },
                Matchers.containsInAnyOrder("ms-0/2/0.46", "ms-0/2/0.56")
        )
    }

    @Test
    fun testReadCurrentAttributesForType() {
        val vrfName = "THU"
        val ifName = "ae2220.46"
        val id = IIDs.NETWORKINSTANCES
                .child(NetworkInstance::class.java, NetworkInstanceKey(vrfName))
                .child(Interfaces::class.java)
                .child(Interface::class.java, InterfaceKey(ifName))
        val builder = InterfaceBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.id, CoreMatchers.sameInstance(ifName))
    }
}