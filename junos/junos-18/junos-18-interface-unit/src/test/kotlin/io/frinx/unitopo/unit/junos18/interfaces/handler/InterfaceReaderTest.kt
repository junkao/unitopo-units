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

package io.frinx.unitopo.unit.junos18.interfaces.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey

class InterfaceReaderTest {

    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private lateinit var reader: InterfaceReader

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        reader = InterfaceReader(underlayAccess)
    }

    @Test
    fun testGetAllIds() {
        val id = IIDs.IN_INTERFACE

        val result = reader.getAllIds(id, readContext)

        Assert.assertThat(
            result.map { it.name },
            Matchers.containsInAnyOrder("ms-0/2/0", "ae2220", "fxp0")
        )
    }

    @Test
    fun testReadCurrentAttributes() {
        val ifName = "ae2220"
        val id = IIDs.INTERFACES.child(Interface::class.java, InterfaceKey(ifName))
        val builder = InterfaceBuilder()

        reader.readCurrentAttributes(id, builder, readContext)

        Assert.assertEquals(ifName, builder.name)
    }
}