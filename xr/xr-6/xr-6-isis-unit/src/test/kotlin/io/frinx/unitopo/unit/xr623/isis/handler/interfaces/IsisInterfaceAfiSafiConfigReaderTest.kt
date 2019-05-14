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
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisIfAfConfAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.AFITYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.IPV6
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.SAFITYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._if.global.afi.safi.list.AfKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._if.global.afi.safi.list.af.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.ISIS
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.utils.IidUtils

class IsisInterfaceAfiSafiConfigReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: IsisInterfaceAfiSafiConfigReader

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        private val INSTANCE_NAME = "ISIS-001"
        private val INTERFACE_ID = "Bundle-Ether4001"
        private val CONFIG_IID = IidUtils.createIid(IIDs.NE_NE_PR_PR_IS_IN_IN_AF_AF_CONFIG,
                NetworInstance.DEFAULT_NETWORK,
                ProtocolKey(ISIS::class.java, INSTANCE_NAME),
                InterfaceKey(InterfaceId(INTERFACE_ID)),
                AfKey(IPV6::class.java, UNICAST::class.java))
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = IsisInterfaceAfiSafiConfigReader(underlayAccess)
    }

    @Test
    fun readCurrentAttributes() {
        val builder = ConfigBuilder()
        target.readCurrentAttributes(CONFIG_IID, builder, readContext)

        assertThat(builder.afiName, CoreMatchers.equalTo(IPV6::class.java) as Matcher<Class<out AFITYPE>>)
        assertThat(builder.safiName, CoreMatchers.equalTo(UNICAST::class.java) as Matcher<Class<out SAFITYPE>>)

        assertThat(builder.getAugmentation(IsisIfAfConfAug::class.java).metric, CoreMatchers.`is`(30L))
    }
}