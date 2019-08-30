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

package io.frinx.unitopo.unit.xr66.isis.handler.global

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisGlobalConfAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisInternalLevel
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.global.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.global.base.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.top.Isis
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.top.isis.Global
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.ISIS
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.hamcrest.Matchers

class IsisGlobalConfigReaderTest : AbstractNetconfHandlerTest() {

    @Mock
    private lateinit var underlayAccess: UnderlayAccess
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var target: IsisGlobalConfigReader

    companion object {
        public val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        public val IID_CONFIG = InstanceIdentifier
                .create(NetworkInstances::class.java)
                .child(NetworkInstance::class.java, NetworkInstanceKey("default"))
                .child(Protocols::class.java)
                .child(Protocol::class.java, ProtocolKey(ISIS::class.java, "ISIS-001"))
                .child(Isis::class.java)
                .child(Global::class.java)
                .child(Config::class.java)
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = IsisGlobalConfigReader(underlayAccess!!)
    }

    @Test
    fun testreadCurrentAttributes() {
        val builder = ConfigBuilder()
        target!!.readCurrentAttributes(IID_CONFIG, builder, readContext!!)
        val list = builder.getAugmentation(IsisGlobalConfAug::class.java)?.maxLinkMetric
        Assert.assertThat(list,
            Matchers.containsInAnyOrder(IsisInternalLevel.LEVEL1,
                IsisInternalLevel.LEVEL2,
                IsisInternalLevel.NOTSET))
    }
}