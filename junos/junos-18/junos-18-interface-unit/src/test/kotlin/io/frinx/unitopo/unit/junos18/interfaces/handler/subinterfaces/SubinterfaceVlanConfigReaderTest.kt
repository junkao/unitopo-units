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
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.Subinterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.Subinterface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.Vlan
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.VlanBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.vlan.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.vlan.ConfigBuilder
import io.frinx.openconfig.openconfig.vlan.IIDs as VlanIIDs

class SubinterfaceVlanConfigReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private val target = SubinterfaceVlanConfigReader(underlayAccess)

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testReadCurrentAttributes_VlanID() {
        val ifName = "ae2220"
        val subIfIndex = 0L
        val id = IIDs.INTERFACES.child(Interface::class.java, InterfaceKey(ifName))
            .child(Subinterfaces::class.java)
            .child(Subinterface::class.java, SubinterfaceKey(subIfIndex))
            .augmentation(Subinterface1::class.java)
            .child(Vlan::class.java)
            .child(Config::class.java)

        val builder = ConfigBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.vlanId.vlanId.value, CoreMatchers.`is`(4000))
    }

    @Test
    fun testReadCurrentAttributes_VlanTag() {
        val ifName = "ae2220"
        val subIfIndex = 46L
        val id = IIDs.INTERFACES.child(Interface::class.java, InterfaceKey(ifName))
            .child(Subinterfaces::class.java)
            .child(Subinterface::class.java, SubinterfaceKey(subIfIndex))
            .augmentation(Subinterface1::class.java)
            .child(Vlan::class.java)
            .child(Config::class.java)

        val builder = ConfigBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.vlanId.qinqId.value, CoreMatchers.equalTo("1061.1636"))
    }

    @Test
    fun testMerge() {
        val config = Mockito.mock(Config::class.java)
        val parentBuilder = VlanBuilder()

        target.merge(parentBuilder, config)

        Assert.assertThat(parentBuilder.config, CoreMatchers.sameInstance(config))
    }

    @Test
    fun testGetBuilder() {
        val result = target.getBuilder(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VL_CONFIG)

        Assert.assertThat(result, CoreMatchers.instanceOf(ConfigBuilder::class.java))
    }
}