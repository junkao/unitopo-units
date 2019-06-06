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

package io.frinx.unitopo.unit.xr66.interfaces.handler

import io.fd.honeycomb.translate.read.ReadContext
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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.Interface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.Damping
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.damping.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.damping.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceDampingConfigReaderTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: InterfaceDampingConfigReader

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes2.xml")
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = InterfaceDampingConfigReader(underlayAccess)
    }

    @Test
    fun testReadCurrentAttributesGigabitEthernet() {
        val ifName = "GigabitEthernet0/0/0/0"
        val halfLife = 10L
        val reuse = 11L
        val suppress = 12L
        val maxsuppress = 13L
        val configBuilder = ConfigBuilder()
        val id = InstanceIdentifier
            .create(Interfaces::class.java)
            .child(Interface::class.java, InterfaceKey(ifName)).augmentation(Interface1::class.java)
            .child(Damping::class.java).child(Config::class.java)

        target.readCurrentAttributes(id, configBuilder, readContext)
        Assert.assertThat(configBuilder.halfLife, CoreMatchers.equalTo(halfLife))
        Assert.assertThat(configBuilder.maxSuppress, CoreMatchers.equalTo(maxsuppress))
        Assert.assertThat(configBuilder.reuse, CoreMatchers.equalTo(reuse))
        Assert.assertThat(configBuilder.suppress, CoreMatchers.equalTo(suppress))
    }

    @Test
    fun testReadCurrentAttributesBundleEther() {
        val ifName = "Bundle-Ether302"
        val halfLife = 20L
        val maxsuppress = 23L
        val reuse = 21L
        val suppress = 22L
        val configBuilder = ConfigBuilder()
        val id = InstanceIdentifier
            .create(Interfaces::class.java)
            .child(Interface::class.java, InterfaceKey(ifName)).augmentation(Interface1::class.java)
            .child(Damping::class.java).child(Config::class.java)

        target.readCurrentAttributes(id, configBuilder, readContext)
        Assert.assertThat(configBuilder.halfLife, CoreMatchers.equalTo(halfLife))
        Assert.assertThat(configBuilder.maxSuppress, CoreMatchers.equalTo(maxsuppress))
        Assert.assertThat(configBuilder.reuse, CoreMatchers.equalTo(reuse))
        Assert.assertThat(configBuilder.suppress, CoreMatchers.equalTo(suppress))
    }

    @Test
    fun testReadCurrentAttributesOtherInterface() {
        val ifName = "Loopback0"
        val configBuilder = ConfigBuilder()
        val id = InstanceIdentifier
            .create(Interfaces::class.java)
            .child(Interface::class.java, InterfaceKey(ifName)).augmentation(Interface1::class.java)
            .child(Damping::class.java).child(Config::class.java)

        target.readCurrentAttributes(id, configBuilder, readContext)
        Assert.assertThat(configBuilder.halfLife, CoreMatchers.nullValue())
        Assert.assertThat(configBuilder.maxSuppress, CoreMatchers.nullValue())
        Assert.assertThat(configBuilder.reuse, CoreMatchers.nullValue())
        Assert.assertThat(configBuilder.suppress, CoreMatchers.nullValue())
    }
}