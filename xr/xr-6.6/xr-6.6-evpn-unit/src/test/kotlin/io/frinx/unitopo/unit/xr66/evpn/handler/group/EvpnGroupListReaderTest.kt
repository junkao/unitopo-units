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

package io.frinx.unitopo.unit.xr7.evpn.handler.group

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.evpn.IIDs
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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.Group
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.GroupBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.GroupKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.ConfigBuilder

class EvpnGroupListReaderTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: EvpnGroupListReader

    companion object {
        val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = Mockito.spy(EvpnGroupListReader(underlayAccess))
    }

    @Test
    fun testGetAllIds() {
        val list = target.getAllIds(IIDs.EV_GR_GROUP, readContext)
        Assert.assertThat(list.map { it.id },
            Matchers.containsInAnyOrder(1L, 2L))
    }

    @Test
    fun testReadCurrentAttributes() {
        val expected = GroupBuilder().apply {
            this.id = 1
            this.config = ConfigBuilder().apply {
                this.id = 1
            }.build()
        }.build()

        val builder = GroupBuilder()
        val IID_GROUP = IIDs.EV_GROUPS.child(Group::class.java, GroupKey(1))
        target.readCurrentAttributes(IID_GROUP, builder, readContext)
        Assert.assertThat(builder.build(), CoreMatchers.equalTo(expected))
    }
}