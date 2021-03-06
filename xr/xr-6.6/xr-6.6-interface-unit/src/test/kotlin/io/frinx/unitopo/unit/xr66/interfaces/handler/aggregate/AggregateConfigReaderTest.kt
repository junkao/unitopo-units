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

package io.frinx.unitopo.unit.xr66.interfaces.handler.aggregate

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.ext.rev180926.IfLagAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Interface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.Aggregation
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.AggregationBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress

class AggregateConfigReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private lateinit var target: AggregateConfigReader

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        target = AggregateConfigReader(underlayAccess)
    }

    @Test
    fun testReadCurrentAttributes() {
        val ifName = "Bundle-Ether3000"
        val systemMac = MacAddress("00:00:00:00:08:00")
        val macAddr = MacAddress("02:8a:96:00:00:00")
        val id = IIDs.INTERFACES
            .child(Interface::class.java, InterfaceKey(ifName))
            .augmentation(Interface1::class.java)
            .child(Aggregation::class.java)
            .child(Config::class.java)
        val builder = ConfigBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.getAugmentation(IfLagAug::class.java).systemIdMac, CoreMatchers.equalTo(systemMac))
        Assert.assertThat(builder.getAugmentation(IfLagAug::class.java).macAddress, CoreMatchers.equalTo(macAddr))
    }

    @Test
    fun testMerge() {
        val config = Mockito.mock(Config::class.java)
        val parentBuilder = AggregationBuilder()
        target.merge(parentBuilder, config)
        Assert.assertThat(parentBuilder.config, CoreMatchers.sameInstance(config))
    }

    @Test
    fun testGetBuilder() {
        val result = target.getBuilder(IIDs.IN_IN_AUG_INTERFACE1_AG_CONFIG)
        Assert.assertThat(result, CoreMatchers.instanceOf(ConfigBuilder::class.java))
    }
}