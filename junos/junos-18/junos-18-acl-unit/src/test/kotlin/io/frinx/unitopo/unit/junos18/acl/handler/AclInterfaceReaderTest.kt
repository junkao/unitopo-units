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

package io.frinx.unitopo.unit.junos18.acl.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.acl.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId

class AclInterfaceReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private lateinit var target: AclInterfaceReader

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        target = AclInterfaceReader(underlayAccess)
    }

    @Test
    fun testGetAllIds() {
        val id = IIDs.AC_IN_INTERFACE

        val result = target.getAllIds(id, readContext)

        Assert.assertThat(
                result.map { it.id.value },
                Matchers.containsInAnyOrder("ae2220.46", "ae2220.56", "ae2220.66")
        )
    }

    @Test
    fun testReadCurrentAttributes() {
        val ifId = "ae2220.46"
        val ifaceKey = InterfaceKey(InterfaceId(ifId))
        val id = IIDs.AC_INTERFACES.child(Interface::class.java, ifaceKey)
        val builder = InterfaceBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.key, CoreMatchers.sameInstance(ifaceKey))
        Assert.assertThat(builder.id.value, CoreMatchers.sameInstance(ifId))
    }

    @Test
    fun testMerge() {
        val parentBuilder = InterfacesBuilder()
        val data: List<Interface> = emptyList()

        target.merge(parentBuilder, data)

        Assert.assertThat(parentBuilder.`interface`, CoreMatchers.sameInstance(data))
    }

    @Test
    fun testGetBuilder() {
        val result = target.getBuilder(IIDs.AC_IN_INTERFACE)

        Assert.assertThat(result, CoreMatchers.instanceOf(InterfaceBuilder::class.java))
    }
}