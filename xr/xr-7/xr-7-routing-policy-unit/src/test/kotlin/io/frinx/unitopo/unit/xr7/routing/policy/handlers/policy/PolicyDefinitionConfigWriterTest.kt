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

package io.frinx.unitopo.unit.xr7.routing.policy.handlers.policy

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.policy.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev190405.RoutingPolicy
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev190405.routing.policy.RoutePolicies
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev190405.routing.policy.route.policies.RoutePolicy
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev190405.routing.policy.route.policies.RoutePolicyKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.definitions.top.policy.definitions.PolicyDefinitionKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.definitions.top.policy.definitions.policy.definition.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.utils.IidUtils
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class PolicyDefinitionConfigWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext
    private lateinit var underlayAccess: UnderlayAccess
    private lateinit var target: PolicyDefinitionConfigWriter

    private lateinit var idCap: ArgumentCaptor<IID<RoutePolicy>>
    private lateinit var dataCap: ArgumentCaptor<RoutePolicy>

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")

        private val POLICY_NAME = "ALL-PERMIT"
        private val POLICY_KEY = PolicyDefinitionKey(POLICY_NAME)
        private val IID_CONFIG = IidUtils.createIid(IIDs.RO_PO_PO_CONFIG, POLICY_KEY)
        private val EMPTY_POLICY = "route-policy $POLICY_NAME\nend-policy"

        private val CONFIG = ConfigBuilder()
            .setName(POLICY_NAME)
            .build()

        private val NATIVE_IID = IID
            .create(RoutingPolicy::class.java)
            .child(RoutePolicies::class.java)
            .child(RoutePolicy::class.java, RoutePolicyKey(CiscoIosXrString(POLICY_NAME)))
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = PolicyDefinitionConfigWriter(underlayAccess)

        idCap = ArgumentCaptor.forClass(IID::class.java) as ArgumentCaptor<IID<RoutePolicy>>
        dataCap = ArgumentCaptor.forClass(RoutePolicy::class.java)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val config = ConfigBuilder(CONFIG)
            .build()
        val id = IID_CONFIG
        Mockito.doNothing().`when`(underlayAccess).safePut(Mockito.any(), Mockito.any())

        target.writeCurrentAttributes(id, config, writeContext)

        Mockito.verify(underlayAccess).safePut(idCap.capture(), dataCap.capture())

        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        Assert.assertThat(idCap.allValues[0], CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<RoutePolicy>>)
        Assert.assertThat(dataCap.allValues[0].routePolicyName.value, CoreMatchers.equalTo(POLICY_NAME))
        Assert.assertThat(dataCap.allValues[0].rplRoutePolicy.value, CoreMatchers.equalTo(EMPTY_POLICY))
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val config = ConfigBuilder(CONFIG)
            .build()
        val id = IID_CONFIG
        Mockito.doNothing().`when`(underlayAccess).safeDelete(Mockito.any(), Mockito.any())

        target.deleteCurrentAttributes(id, config, writeContext)

        Mockito.verify(underlayAccess).safeDelete(idCap.capture(), dataCap.capture())

        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        Assert.assertThat(idCap.allValues[0], CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<RoutePolicy>>)
        Assert.assertThat(dataCap.allValues[0].routePolicyName.value, CoreMatchers.equalTo(POLICY_NAME))
        Assert.assertThat(dataCap.allValues[0].rplRoutePolicy.value, CoreMatchers.equalTo(EMPTY_POLICY))
    }
}