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

package io.frinx.unitopo.unit.xr7.bgp.handler.neighbor

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import io.frinx.unitopo.unit.xr7.bgp.handler.toOpenconfig
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev170626.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.AfiSafis
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.AfiSafisBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.Bgp
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.Neighbors
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv4Address
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NeighborAfiSafiReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var target: NeighborAfiSafiReader

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/bgp-conf4.xml")

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        target = NeighborAfiSafiReader(underlayAccess)
    }

    @Test
    fun testReadCurrentAttributes() {
        val protKey = ProtocolKey(BGP::class.java, "default")
        val vrfKey = NetworInstance.DEFAULT_NETWORK
        val ipAddress = IpAddress(Ipv4Address("10.2.2.1"))
        val neighborKey = NeighborKey(ipAddress)
        val afiSafiName = BgpAddressFamily.L2vpnEvpn.toOpenconfig()

        val id = InstanceIdentifier.create(NetworkInstances::class.java)
                .child(NetworkInstance::class.java, vrfKey)
                .child(Protocols::class.java)
                .child(Protocol::class.java, protKey)
                .child(Bgp::class.java)
                .child(Neighbors::class.java)
                .child(Neighbor::class.java, neighborKey)
                .child(AfiSafis::class.java)
                .child(AfiSafi::class.java, AfiSafiKey(afiSafiName))

        val builder = AfiSafiBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.config.afiSafiName, CoreMatchers.equalTo(afiSafiName))
    }

    @Test
    fun testMerge() {
        val list: List<AfiSafi> = emptyList()
        val data: MutableList<AfiSafi> = list.toMutableList()
        val parentBuilder = AfiSafisBuilder()

        target.merge(parentBuilder, data)

        Assert.assertThat(parentBuilder.afiSafi, CoreMatchers.sameInstance(data))
    }

    @Test
    fun testGetBuilder() {
        val result = target.getBuilder(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AFISAFI)

        Assert.assertThat(result, CoreMatchers.instanceOf(AfiSafiBuilder::class.java))
    }
}