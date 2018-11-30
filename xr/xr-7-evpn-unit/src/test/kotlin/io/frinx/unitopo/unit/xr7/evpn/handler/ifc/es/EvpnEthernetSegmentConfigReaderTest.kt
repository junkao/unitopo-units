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
import io.frinx.unitopo.unit.xr7.evpn.handler.ifc.es.EvpnEthernetSegmentConfigReader
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.EthernetSegment
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.ethernet.segment.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.ethernet.segment.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.types.rev181112.EthernetSegmentIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.types.rev181112.PORTACTIVE
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress

class EvpnEthernetSegmentConfigReaderTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: EvpnEthernetSegmentConfigReader

    companion object {
        val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = Mockito.spy(EvpnEthernetSegmentConfigReader(underlayAccess))
    }

    @Test
    fun testReadCurrentAttributes() {
        val builder = ConfigBuilder()
        val id = IIDs.EV_INTERFACES
            .child(Interface::class.java, InterfaceKey("Bundle-Ether20001"))
            .child(EthernetSegment::class.java)
            .child(Config::class.java)
        target.readCurrentAttributes(id, builder, readContext)
        Assert.assertTrue(builder.identifier
            .equals(EthernetSegmentIdentifier("11:22:33:44:55:66:77:88:99")))
        Assert.assertTrue(builder.bgpRouteTarget.equals(MacAddress("ff:00:00:ff:00:00")))
        Assert.assertTrue(builder.loadBalancingMode.equals(PORTACTIVE::class.java))
    }
}