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

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.policy.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.definitions.top.policy.definitions.PolicyDefinitionKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.policy.definitions.top.policy.definitions.policy.definition.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.utils.IidUtils

class PolicyDefinitionConfigReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: PolicyDefinitionConfigReader

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        private val POLICY_NAME = "ALL-PERMIT"
        private val POLICY_KEY = PolicyDefinitionKey(POLICY_NAME)
        private val ID = IidUtils.createIid(IIDs.RO_PO_PO_CONFIG, POLICY_KEY)
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = PolicyDefinitionConfigReader(underlayAccess)
    }

    @Test
    fun testReadCurrentAttributes() {
        val builder = ConfigBuilder()
        target.readCurrentAttributes(ID, builder, readContext)

        Assert.assertThat(builder.name, CoreMatchers.sameInstance(POLICY_NAME))
    }
}