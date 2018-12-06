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

package io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.Subinterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey

class SubinterfaceReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private val target = SubinterfaceReader(underlayAccess)

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testGetAllIds() {
        val interfaceName = "ae2220"
        val id = IIDs.INTERFACES.child(Interface::class.java, InterfaceKey(interfaceName))
            .child(Subinterfaces::class.java)
            .child(Subinterface::class.java)

        val result = target.getAllIds(id, readContext)

        Assert.assertThat(
            result.map { it.index },
            Matchers.containsInAnyOrder(0L, 46L)
        )
    }

    @Test
    fun testReadCurrentAttributes() {
        val ifName = "ae2220"
        val subIfIndex = 0L
        val id = IIDs.INTERFACES.child(Interface::class.java, InterfaceKey(ifName))
            .child(Subinterfaces::class.java)
            .child(Subinterface::class.java, SubinterfaceKey(subIfIndex))
        val builder = SubinterfaceBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.index, CoreMatchers.equalTo(subIfIndex))
    }

    @Test
    fun testMerge() {
        val parentBuilder = SubinterfacesBuilder()
        val data: List<Subinterface> = emptyList()

        target.merge(parentBuilder, data)

        Assert.assertThat(parentBuilder.subinterface, CoreMatchers.sameInstance(data))
    }

    @Test
    fun testGetBuilder() {
        val result = target.getBuilder(IIDs.IN_IN_SU_SUBINTERFACE)

        Assert.assertThat(result, CoreMatchers.instanceOf(SubinterfaceBuilder::class.java))
    }
}