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
import io.frinx.openconfig.openconfig.evpn.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import io.frinx.unitopo.unit.xr7.evpns.handler.EvpnGroupCoreInterfaceListReader
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn._interface.part.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.Group
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.GroupKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.CoreInterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.core.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.core.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.core.interfaces.InterfaceKey

class EvpnGroupCoreInterfaceListReaderTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: EvpnGroupCoreInterfaceListReader

    companion object {
        val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = Mockito.spy(EvpnGroupCoreInterfaceListReader(underlayAccess))
    }

    @Test
    fun testGetAllIds() {
        val IID_CO_IFC = IIDs.EV_GROUPS
            .child(Group::class.java, GroupKey(1))
            .child(CoreInterfaces::class.java)
            .child(Interface::class.java)

        val list = target.getAllIds(IID_CO_IFC, readContext)
        Assert.assertThat(list.map { it.name },
            Matchers.containsInAnyOrder("Bundle-Ether11001", "Bundle-Ether11002"))
    }

    @Test
    fun testReadCurrentAttributes() {
        val expected = InterfaceBuilder().apply {
            this.name = "Bundle-Ether11001"
            this.config = ConfigBuilder().apply {
                this.name = "Bundle-Ether11001"
            }.build()
        }.build()

        val builder = InterfaceBuilder()
        val IID_CO_IFC = IIDs.EV_GROUPS
            .child(Group::class.java, GroupKey(1))
            .child(CoreInterfaces::class.java)
            .child(Interface::class.java, InterfaceKey("Bundle-Ether11001"))

        target.readCurrentAttributes(IID_CO_IFC, builder, readContext)
        Assert.assertThat(builder.build(), CoreMatchers.equalTo(expected))
    }
}