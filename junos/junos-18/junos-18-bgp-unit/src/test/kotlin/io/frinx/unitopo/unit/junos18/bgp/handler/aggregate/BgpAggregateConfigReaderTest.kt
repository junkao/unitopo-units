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

package io.frinx.unitopo.unit.junos18.bgp.handler.aggregate

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.NiProtAggAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.LocalAggregates
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.Aggregate
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpPrefixBuilder

class BgpAggregateConfigReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private val target = BgpAggregateConfigReader(underlayAccess)

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testReadCurrentAttributesForType() {
        val vrfName = "THU"
        val prefix = IpPrefixBuilder.getDefaultInstance("10.128.178.64/26")
        val id = IIDs.NETWORKINSTANCES
                .child(NetworkInstance::class.java, NetworkInstanceKey(vrfName))
                .child(Protocols::class.java)
                .child(Protocol::class.java, ProtocolKey(BGP::class.java, "default"))
                .child(LocalAggregates::class.java)
                .child(Aggregate::class.java, AggregateKey(prefix))
                .child(Config::class.java)

        val builder = ConfigBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        val augBuilder = builder.getAugmentation(NiProtAggAug::class.java)!!

        Assert.assertThat(builder.prefix, CoreMatchers.sameInstance(prefix))
        Assert.assertThat(augBuilder.isSummaryOnly, CoreMatchers.equalTo(true))
        Assert.assertThat(augBuilder.applyPolicy, CoreMatchers.equalTo(listOf("AGGREGATE1", "AGGREGATE2")))
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testMerge() {
        val config = Mockito.mock(Config::class.java)
        val parentBuilder = AggregateBuilder()

        target.merge(parentBuilder, config)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testGetBuilder() {
        target.getBuilder(IIDs.NE_NE_PR_PR_LO_AG_CONFIG)
    }
}