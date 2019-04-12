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

package io.frinx.unitopo.unit.xr623.isis.handler.interfaces

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.top.Isis
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.top.isis.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.ISIS
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class IsisInterfaceReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: IsisInterfaceReader

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        private val INSTANCE_NAME = "ISIS-001"
        private val INTERFACE_ID = "Bundle-Ether4001"
        private val INTERFACES_IID = InstanceIdentifier
                .create(NetworkInstances::class.java)
                .child(NetworkInstance::class.java, NetworInstance.DEFAULT_NETWORK)
                .child(Protocols::class.java)
                .child(Protocol::class.java, ProtocolKey(ISIS::class.java, INSTANCE_NAME))
                .child(Isis::class.java)
                .child(Interfaces::class.java)
        private val INTERFACE_IID = INTERFACES_IID
                .child(Interface::class.java, InterfaceKey(InterfaceId(INTERFACE_ID)))
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = IsisInterfaceReader(underlayAccess)
    }

    @Test
    fun getAllIdsForType() {
        val iid = INTERFACES_IID.child(Interface::class.java)
        val expected = listOf("Bundle-Ether4001", "Bundle-Ether5001", "Bundle-Ether5002")
                .map { InterfaceKey(InterfaceId(it)) }

        val result = target.getAllIds(iid, readContext)

        Assert.assertThat(result.sortedBy { it.interfaceId.value }, CoreMatchers.equalTo(expected))
    }

    @Test
    fun readCurrentAttributesForType() {
        val builder = InterfaceBuilder()

        target.readCurrentAttributes(INTERFACE_IID, builder, readContext)

        Assert.assertThat(builder.interfaceId.value, CoreMatchers.sameInstance(INTERFACE_ID))
    }
}