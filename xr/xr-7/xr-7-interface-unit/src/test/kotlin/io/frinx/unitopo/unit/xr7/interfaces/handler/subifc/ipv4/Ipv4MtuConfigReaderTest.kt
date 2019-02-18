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

package io.frinx.unitopo.unit.xr7.interfaces.handler.subifc.ipv4

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.openconfig.openconfig._if.ip.IIDs as IfIpIIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.Ipv4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.Ipv4Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.Subinterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.ConfigBuilder

class Ipv4MtuConfigReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private lateinit var target: Ipv4MtuConfigReader

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        target = Ipv4MtuConfigReader(underlayAccess)
    }

    @Test
    fun testReadCurrentAttributes() {
        val ifName = "GigabitEthernet0/0/0/0"
        val mtu = 65535
        val id = IIDs.INTERFACES
                .child(Interface::class.java, InterfaceKey(ifName))
                .child(Subinterfaces::class.java)
                .child(Subinterface::class.java, SubinterfaceKey(0L))
                .augmentation(Subinterface1::class.java)
                .child(Ipv4::class.java)
                .child(Config::class.java)
        val builder = ConfigBuilder()
        target.readCurrentAttributes(id, builder, readContext)
        Assert.assertEquals(builder.mtu, mtu)
    }

    @Test
    fun testMerge() {
        val config = Mockito.mock(Config::class.java)
        val parentBuilder = Ipv4Builder()
        target.merge(parentBuilder, config)
        Assert.assertThat(parentBuilder.config, CoreMatchers.sameInstance(config))
    }

    @Test
    fun testGetBuilder() {
        val result = target.getBuilder(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_CONFIG)
        Assert.assertThat(result, CoreMatchers.instanceOf(ConfigBuilder::class.java))
    }
}