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

package io.frinx.unitopo.unit.xr66.routing.policy.handlers.policy

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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev170907.RoutingPolicy
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev170907.routing.policy.RoutePolicies
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev170907.routing.policy.route.policies.RoutePolicy
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.policy.repository.cfg.rev170907.routing.policy.route.policies.RoutePolicyKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.PolicyResultType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.actions.top.ActionsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.definitions.top.policy.definitions.PolicyDefinitionKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.statements.top.StatementsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.statements.top.statements.StatementBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.utils.IidUtils
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.actions.top.actions.ConfigBuilder as ActionConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.statements.top.statements.statement.ConfigBuilder as StatementConfigBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class StatementsWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext
    private lateinit var underlayAccess: UnderlayAccess
    private lateinit var target: StatementsWriter

    private lateinit var idCaps: Array<ArgumentCaptor<IID<RoutePolicy>>>
    private lateinit var dataCaps: Array<ArgumentCaptor<RoutePolicy>>

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")

        private val POLICY_NAME = "ALL-PERMIT"
        private val STATEMENT_NAME = "1"
        private val POLICY_KEY = PolicyDefinitionKey(POLICY_NAME)
        private val IID_STETEMENTS = IidUtils.createIid(IIDs.RO_PO_PO_STATEMENTS, POLICY_KEY)
        private val EMPTY_POLICY = "route-policy $POLICY_NAME\nend-policy"
        private val PASSROUTE_POLICY = "route-policy $POLICY_NAME\n  pass\nend-policy"
        private val REJECTROUTE_POLICY = "route-policy $POLICY_NAME\n  drop\nend-policy"

        private val PASSROUTE_STATEMENTS = StatementsBuilder()
            .setStatement(listOf(StatementBuilder()
                .setName(STATEMENT_NAME)
                .setConfig(StatementConfigBuilder()
                    .setName(STATEMENT_NAME)
                    .build())
                .setActions(ActionsBuilder()
                    .setConfig(ActionConfigBuilder()
                        .setPolicyResult(PolicyResultType.PASSROUTE)
                        .build())
                    .build())
                .build()))
            .build()

        private val REJECTROUTE_STATEMENTS = StatementsBuilder()
            .setStatement(listOf(StatementBuilder()
                .setName(STATEMENT_NAME)
                .setConfig(StatementConfigBuilder()
                    .setName(STATEMENT_NAME)
                    .build())
                .setActions(ActionsBuilder()
                    .setConfig(ActionConfigBuilder()
                        .setPolicyResult(PolicyResultType.REJECTROUTE)
                        .build())
                    .build())
                .build()))
            .build()

        private val EMPTY_POLICY_STATEMENTS = StatementsBuilder()
            .setStatement(listOf(StatementBuilder()
                .setName(POLICY_NAME)
                .setConfig(StatementConfigBuilder()
                    .setName(STATEMENT_NAME)
                    .build())
                .build()))
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
        target = StatementsWriter(underlayAccess)

        idCaps = arrayOf(ArgumentCaptor.forClass(IID::class.java) as ArgumentCaptor<IID<RoutePolicy>>,
            ArgumentCaptor.forClass(IID::class.java) as ArgumentCaptor<IID<RoutePolicy>>)
        dataCaps = arrayOf(ArgumentCaptor.forClass(RoutePolicy::class.java),
            ArgumentCaptor.forClass(RoutePolicy::class.java))
    }

    @Test
    fun testWriteCurrentAttributes() {
        val data = StatementsBuilder(PASSROUTE_STATEMENTS)
            .build()
        val id = IID_STETEMENTS
        Mockito.doNothing().`when`(underlayAccess).safeMerge(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())

        target.writeCurrentAttributes(id, data, writeContext)

        Mockito.verify(underlayAccess)
            .safeMerge(idCaps[0].capture(), dataCaps[0].capture(), idCaps[1].capture(), dataCaps[1].capture())

        Assert.assertThat(idCaps[0].allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCaps[0].allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(idCaps[1].allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCaps[1].allValues.size, CoreMatchers.`is`(1))

        Assert.assertThat(idCaps[0].allValues[0], CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<RoutePolicy>>)
        Assert.assertThat(dataCaps[0].allValues[0].routePolicyName.value, CoreMatchers.equalTo(POLICY_NAME))
        Assert.assertThat(dataCaps[0].allValues[0].rplRoutePolicy.value, CoreMatchers.equalTo(EMPTY_POLICY))

        Assert.assertThat(idCaps[1].allValues[0], CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<RoutePolicy>>)
        Assert.assertThat(dataCaps[1].allValues[0].routePolicyName.value, CoreMatchers.equalTo(POLICY_NAME))
        Assert.assertThat(dataCaps[1].allValues[0].rplRoutePolicy.value, CoreMatchers.equalTo(PASSROUTE_POLICY))
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val dataBefore = StatementsBuilder(PASSROUTE_STATEMENTS)
            .build()
        val dataAfter = StatementsBuilder(REJECTROUTE_STATEMENTS)
            .build()
        val id = IID_STETEMENTS
        Mockito.doNothing().`when`(underlayAccess).safeMerge(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())

        target.updateCurrentAttributes(id, dataBefore, dataAfter, writeContext)

        Mockito.verify(underlayAccess)
            .safeMerge(idCaps[0].capture(), dataCaps[0].capture(), idCaps[1].capture(), dataCaps[1].capture())

        Assert.assertThat(idCaps[0].allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCaps[0].allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(idCaps[1].allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCaps[1].allValues.size, CoreMatchers.`is`(1))

        Assert.assertThat(idCaps[0].allValues[0], CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<RoutePolicy>>)
        Assert.assertThat(dataCaps[0].allValues[0].routePolicyName.value, CoreMatchers.equalTo(POLICY_NAME))
        Assert.assertThat(dataCaps[0].allValues[0].rplRoutePolicy.value, CoreMatchers.equalTo(PASSROUTE_POLICY))

        Assert.assertThat(idCaps[1].allValues[0], CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<RoutePolicy>>)
        Assert.assertThat(dataCaps[1].allValues[0].routePolicyName.value, CoreMatchers.equalTo(POLICY_NAME))
        Assert.assertThat(dataCaps[1].allValues[0].rplRoutePolicy.value, CoreMatchers.equalTo(REJECTROUTE_POLICY))
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val data = StatementsBuilder(PASSROUTE_STATEMENTS)
            .build()
        val id = IID_STETEMENTS
        Mockito.doNothing().`when`(underlayAccess).safeMerge(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())

        target.deleteCurrentAttributes(id, data, writeContext)

        Mockito.verify(underlayAccess)
            .safeMerge(idCaps[0].capture(), dataCaps[0].capture(), idCaps[1].capture(), dataCaps[1].capture())

        Assert.assertThat(idCaps[0].allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCaps[0].allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(idCaps[1].allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCaps[1].allValues.size, CoreMatchers.`is`(1))

        Assert.assertThat(idCaps[0].allValues[0], CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<RoutePolicy>>)
        Assert.assertThat(dataCaps[0].allValues[0].routePolicyName.value, CoreMatchers.equalTo(POLICY_NAME))
        Assert.assertThat(dataCaps[0].allValues[0].rplRoutePolicy.value, CoreMatchers.equalTo(PASSROUTE_POLICY))

        Assert.assertThat(idCaps[1].allValues[0], CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<RoutePolicy>>)
        Assert.assertThat(dataCaps[1].allValues[0].routePolicyName.value, CoreMatchers.equalTo(POLICY_NAME))
        Assert.assertThat(dataCaps[1].allValues[0].rplRoutePolicy.value, CoreMatchers.equalTo(EMPTY_POLICY))
    }
}