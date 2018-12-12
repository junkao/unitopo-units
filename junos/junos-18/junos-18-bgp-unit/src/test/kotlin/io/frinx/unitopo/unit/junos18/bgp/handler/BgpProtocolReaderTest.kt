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

package io.frinx.unitopo.unit.junos18.bgp.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ProtocolsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.INSTALLPROTOCOLTYPE

class BgpProtocolReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private val target = BgpProtocolReader(underlayAccess)

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testGetAllIds() {
        val vrfName = "BLIZZARD"
        val id = IIDs.NETWORKINSTANCES
                .child(NetworkInstance::class.java, NetworkInstanceKey(vrfName))
                .child(Protocols::class.java)
                .child(Protocol::class.java)

        val result = target.getAllIds(id, readContext)

        Assert.assertThat(result.size, CoreMatchers.`is`(1))
        Assert.assertThat(
                result[0].identifier,
                CoreMatchers.equalTo(BGP::class.java) as Matcher<Class<out INSTALLPROTOCOLTYPE>>)
        Assert.assertThat(result[0].name, CoreMatchers.equalTo("default"))
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testReadCurrentAttributesForType() {
        val vrfName = "BLIZZARD"
        val protocol = BGP::class.java
        val protcolName = "default"
        val id = IIDs.NETWORKINSTANCES
                .child(NetworkInstance::class.java, NetworkInstanceKey(vrfName))
                .child(Protocols::class.java)
                .child(Protocol::class.java, ProtocolKey(protocol, protcolName))

        val builder = ProtocolBuilder()

        target.readCurrentAttributesForType(id, builder, readContext)

        Assert.assertThat(
                builder.identifier,
                CoreMatchers.equalTo(BGP::class.java) as Matcher<Class<out INSTALLPROTOCOLTYPE>>)
        Assert.assertThat(builder.name, CoreMatchers.sameInstance(protcolName))
    }

    @Suppress("UNCHECKED_CAST")
    @Test(expected = UnsupportedOperationException::class)
    fun testMerge() {
        val config = Mockito.mock(List::class.java) as List<Protocol>
        val parentBuilder = ProtocolsBuilder()

        target.merge(parentBuilder, config)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testGetBuilder() {
        target.getBuilder(IIDs.NE_NE_PR_PROTOCOL)
    }
}