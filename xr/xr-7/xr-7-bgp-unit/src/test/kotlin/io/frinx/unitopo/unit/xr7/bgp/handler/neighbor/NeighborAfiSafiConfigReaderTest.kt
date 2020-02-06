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
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev190405.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.afi.safi.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.afi.safi.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.AfiSafis
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

class NeighborAfiSafiConfigReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var target: NeighborAfiSafiConfigReader

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/bgp-conf4.xml")

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        target = NeighborAfiSafiConfigReader(underlayAccess)
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
                .child(Config::class.java)

        val builder = ConfigBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.afiSafiName, CoreMatchers.equalTo(afiSafiName))
    }

    @Test
    fun testMerge() {
        val config = Mockito.mock(Config::class.java)
        val parentBuilder = AfiSafiBuilder()

        target.merge(parentBuilder, config)

        Assert.assertThat(parentBuilder.config, CoreMatchers.sameInstance(config))
    }

    @Test
    fun testGetBuilder() {
        val result = target.getBuilder(IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_CONFIG)

        Assert.assertThat(result, CoreMatchers.instanceOf(ConfigBuilder::class.java))
    }
}