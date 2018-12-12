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

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.junit.Test
import org.mockito.Mockito
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.Config

class BgpProtocolConfigWriterTest {
    private var writeContext = Mockito.mock(WriteContext::class.java)
    private val underlayAccess = Mockito.mock(UnderlayAccess::class.java)
    private val target = BgpProtocolConfigWriter(underlayAccess)

    @Test
    fun testWriteCurrentAttributesForType_V3VPN() {
        val vrfName = "BLIZZARD"
        val id = IIDs.NETWORKINSTANCES
                .child(NetworkInstance::class.java, NetworkInstanceKey(vrfName))
                .child(Protocols::class.java)
                .child(Protocol::class.java)
                .child(Config::class.java)
        val config = Mockito.mock(Config::class.java)

        target.writeCurrentAttributesForType(id, config, writeContext)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testWriteurrentAttributesForType_DefaultInstance() {
        val id = IIDs.NETWORKINSTANCES
                .child(NetworkInstance::class.java, NetworInstance.DEFAULT_NETWORK)
                .child(Protocols::class.java)
                .child(Protocol::class.java)
                .child(Config::class.java)
        val config = Mockito.mock(Config::class.java)

        target.writeCurrentAttributesForType(id, config, writeContext)
    }
}