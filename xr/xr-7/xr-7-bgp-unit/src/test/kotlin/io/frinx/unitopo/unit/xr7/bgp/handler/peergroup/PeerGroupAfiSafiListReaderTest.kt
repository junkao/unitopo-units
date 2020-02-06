/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.bgp.handler.peergroup

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import io.frinx.unitopo.unit.xr7.bgp.handler.toOpenconfig
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev190405.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.afi.safi.list.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.base.AfiSafis
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.IPV4UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.L3VPNIPV4UNICAST

class PeerGroupAfiSafiListReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var target: PeerGroupAfiSafiListReader

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/bgp-conf5.xml")

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        target = PeerGroupAfiSafiListReader(underlayAccess)
    }

    @Test
    fun testReadCurrentAttributes() {
        val afiSafiName = BgpAddressFamily.Vpnv4Unicast.toOpenconfig()
        val id = PeerGroupListReaderTest.id
                .child(AfiSafis::class.java)
                .child(AfiSafi::class.java, AfiSafiKey(afiSafiName))

        val builder = AfiSafiBuilder()
        target.readCurrentAttributes(id, builder, readContext)
        Assert.assertThat(builder.config.afiSafiName, CoreMatchers.equalTo(afiSafiName))
    }

    @Test
    fun testGetAllIds() {
        val list = target.getAllIds(
            PeerGroupListReaderTest.id.child(AfiSafis::class.java).child(AfiSafi::class.java),
            readContext)
        Assert.assertThat(list.map { it.afiSafiName },
            Matchers.containsInAnyOrder(L3VPNIPV4UNICAST::class.java, IPV4UNICAST::class.java))
    }
}