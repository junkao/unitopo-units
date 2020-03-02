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

package io.frinx.unitopo.unit.xr7.isis.handler.global

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.Af1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.Redistributions
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.Redistribution
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.RedistributionKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.redistribution.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.redistribution.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.IPV6
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.LevelType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.Af
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.AfKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.global.base.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.top.Isis
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.top.isis.Global
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.ISIS
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class IsisRedistributionConfigReaderTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var underlayAccess: UnderlayAccess

    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var target: IsisRedistributionConfigReader

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        val IID_CONFIG = InstanceIdentifier
            .create(NetworkInstances::class.java)
            .child(NetworkInstance::class.java, NetworkInstanceKey("default"))
            .child(Protocols::class.java)
            .child(Protocol::class.java, ProtocolKey(ISIS::class.java, "ISIS-001"))
            .child(Isis::class.java)
            .child(Global::class.java)
            .child(AfiSafi::class.java)
            .child(Af::class.java, AfKey(IPV6::class.java, UNICAST::class.java))
            .augmentation(Af1::class.java)
            .child(Redistributions::class.java)
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = IsisRedistributionConfigReader(underlayAccess)
    }

    @Test
    fun testReadCurrentAttributes01() {
        val builder = ConfigBuilder()
        val id = IID_CONFIG
            .child(Redistribution::class.java, RedistributionKey("400", "isis"))
            .child(Config::class.java)
        target.readCurrentAttributes(id, builder, readContext)
        val expected = ConfigBuilder()
            .setInstance("400")
            .setProtocol("isis")
            .setLevel(LevelType.LEVEL12)
            .setRoutePolicy("ISIS-REDISTRIBUTE-001")
            .build()
        Assert.assertThat(expected, CoreMatchers.equalTo(builder.build()) as Matcher<in Config>)
    }

    @Test
    fun testReadCurrentAttributes02() {
        val builder = ConfigBuilder()
        val id = IID_CONFIG
            .child(Redistribution::class.java, RedistributionKey("600", "isis"))
            .child(Config::class.java)
        target.readCurrentAttributes(id, builder, readContext)

        // verify parameter to underlay
        val expected = ConfigBuilder()
            .setInstance("600")
            .setProtocol("isis")
            .setLevel(LevelType.LEVEL1)
            .setRoutePolicy("ISIS-REDISTRIBUTE-002")
            .build()
        Assert.assertThat(expected, CoreMatchers.equalTo(builder.build()) as Matcher<in Config>)
    }
}