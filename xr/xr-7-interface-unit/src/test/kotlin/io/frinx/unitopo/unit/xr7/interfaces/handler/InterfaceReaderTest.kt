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

package io.frinx.unitopo.unit.xr7.interfaces.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceReaderTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: InterfaceReader

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = Mockito.spy(InterfaceReader(underlayAccess))
    }

    @Test
    fun testParseIfcType() {
        val expected: Class<out InterfaceType> = Ieee8023adLag::class.java
        Assert.assertThat(parseIfcType("Bundle-Ether100"), CoreMatchers.equalTo(expected))
        Assert.assertThat(
                parseIfcType("GigabitEthernet0/0/0/5"),
                CoreMatchers.equalTo(EthernetCsmacd::class.java as? Class<out InterfaceType>)
        )
    }

    @Test
    fun testGetAllIds() {
        val id = IIDs.IN_INTERFACE

        val result = target.getAllIds(id, readContext)

        Assert.assertThat(
            result.map { it.name },
            Matchers.containsInAnyOrder(
                "GigabitEthernet0/0/0/0",
                "TenGigE0/0/0/2",
                "Bundle-Ether301",
                    "Bundle-Ether302",
                    "Bundle-Ether300"
            )
        )
    }

    @Test
    fun testReadCurrentAttributes() {
        val ifName = "GigabitEthernet0/0/0/0"
        val id = InstanceIdentifier
                .create(Interfaces::class.java)
                .child(Interface::class.java, InterfaceKey(ifName))
        val interfaceBuilder = InterfaceBuilder()

        target.readCurrentAttributes(id, interfaceBuilder, readContext)

        Assert.assertThat(interfaceBuilder.name, CoreMatchers.sameInstance(ifName))
    }
}