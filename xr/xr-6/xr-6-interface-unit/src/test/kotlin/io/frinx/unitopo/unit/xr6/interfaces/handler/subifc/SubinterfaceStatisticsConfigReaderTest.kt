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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.Subinterface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.Statistics
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.StatisticsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.statistics.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.Subinterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.statistics.ConfigBuilder

class SubinterfaceStatisticsConfigReaderTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private lateinit var target: SubinterfaceStatisticsConfigReader

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        target = SubinterfaceStatisticsConfigReader(underlayAccess)
    }

    @Test
    fun testReadCurrentAttributes() {
        val id = IIDs.INTERFACES
            .child(Interface::class.java, InterfaceKey("GigabitEthernet0/0/0/0"))
            .child(Subinterfaces::class.java)
            .child(Subinterface::class.java, SubinterfaceKey(2147483647L))
            .augmentation(Subinterface1::class.java).child(Statistics::class.java)
            .child(Config::class.java)
        val builder = ConfigBuilder()
        target.readCurrentAttributes(id, builder, readContext)
        Assert.assertEquals(builder.loadInterval, 600L)
    }

    @Test
    fun testMerge() {
        val config = Mockito.mock(Config::class.java)
        val parentBuilder = StatisticsBuilder()

        target.merge(parentBuilder, config)

        Assert.assertThat(parentBuilder.config, CoreMatchers.sameInstance(config))
    }
}