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

package io.frinx.unitopo.unit.xr7.bgp.handler.aggregates

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import io.frinx.unitopo.unit.xr7.bgp.handler.aggregate.BgpAggregateReader
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.LocalAggregates
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.Aggregate
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpPrefix
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv4Prefix
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class BgpAggregateReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var target: BgpAggregateReader

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/bgp-conf3.xml")

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        target = BgpAggregateReader(underlayAccess)
    }

    @Test
    fun testReadCurrentAttributes() {
        val protKey = ProtocolKey(BGP::class.java, "default")
        val vrfKey = NetworkInstanceKey("THU")
        val prefix = IpPrefix(Ipv4Prefix("10.11.12.13/32"))
        val aggregateKey = AggregateKey(prefix)

        val id = InstanceIdentifier.create(NetworkInstances::class.java)
                .child(NetworkInstance::class.java, vrfKey)
                .child(Protocols::class.java)
                .child(Protocol::class.java, protKey)
                .child(LocalAggregates::class.java)
                .child(Aggregate::class.java, aggregateKey)

        val builder = AggregateBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.prefix, CoreMatchers.equalTo(prefix))
    }
}