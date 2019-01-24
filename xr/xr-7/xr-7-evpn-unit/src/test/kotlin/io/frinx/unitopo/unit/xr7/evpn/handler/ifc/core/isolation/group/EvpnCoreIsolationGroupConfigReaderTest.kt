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
import io.frinx.unitopo.unit.xr7.evpn.handler.ifc.core.isolation.group.EvpnCoreIsolationGroupConfigReader
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.CoreIsolationGroup
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.core.isolation.group.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.core.isolation.group.ConfigBuilder

class EvpnCoreIsolationGroupConfigReaderTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: EvpnCoreIsolationGroupConfigReader

    companion object {
        val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = Mockito.spy(EvpnCoreIsolationGroupConfigReader(underlayAccess))
    }

    @Test
    fun testReadCurrentAttributes() {
        val builder = ConfigBuilder()
        val id = IIDs.EV_INTERFACES
            .child(Interface::class.java, InterfaceKey("Bundle-Ether20001"))
            .child(CoreIsolationGroup::class.java)
            .child(Config::class.java)
        target.readCurrentAttributes(id, builder, readContext)
        Assert.assertTrue(builder.id == 100L)
    }
}