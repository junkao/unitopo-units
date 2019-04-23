/*
 * Copyright © 2019 Frinx and others.
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

package io.frinx.unitopo.unit.xr623.interfaces.handler.aggregate.bfd

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Interface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.Aggregation
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.IfLagBfdAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.bfd.top.BfdIpv6
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.bfd.top.bfd.ipv6.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.bfd.top.bfd.ipv6.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey

class BfdIpv6ConfigReaderTest {
    @Mock
    private lateinit var readContext: ReadContext
    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private lateinit var target: BfdIpv6ConfigReader

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        target = BfdIpv6ConfigReader(underlayAccess)
    }

    @Test
    fun testReadCurrentAttributes() {
        val ifName = "Bundle-Ether3000"
        val id = IIDs.INTERFACES
            .child(Interface::class.java, InterfaceKey(ifName))
            .augmentation(Interface1::class.java)
            .child(Aggregation::class.java)
            .augmentation(IfLagBfdAug::class.java)
            .child(BfdIpv6::class.java)
            .child(Config::class.java)
        val builder = ConfigBuilder()
        target.readCurrentAttributes(id, builder, readContext)
        Assert.assertEquals("2001::", builder.destinationAddress.value)
        Assert.assertEquals(500L, builder.minInterval)
    }
}