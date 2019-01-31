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

package io.frinx.unitopo.unit.junos18.probes.handler.test

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.probes.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.test.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.test.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.Probe
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.ProbeKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.juniper.rev181203.Config4 as JuniperExtConfigAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.Test as ProbeTest
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.TestBuilder as ProbeTestBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.TestKey as ProbeTestKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.probe.Tests as ProbeTests

class ProbeTestConfigReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private lateinit var target: ProbeTestConfigReader

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        target = ProbeTestConfigReader(underlayAccess)
    }

    @Test
    fun testReadCurrentAttributes() {
        val probeName = "MS00"
        val probeTestName = "ae2220.46_SBM-P-00000827-19"
        val id = IIDs.PROBES
                .child(Probe::class.java, ProbeKey(probeName))
                .child(ProbeTests::class.java)
                .child(ProbeTest::class.java, ProbeTestKey(probeTestName))
                .child(Config::class.java)

        val builder = ConfigBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.name, CoreMatchers.equalTo(probeTestName))
        Assert.assertThat(builder.source.ipv4Address.value, CoreMatchers.equalTo("10.128.178.94"))
        Assert.assertThat(
                builder.getAugmentation(JuniperExtConfigAug::class.java).destinationInterface,
                CoreMatchers.equalTo("ms-0/2/0.46")
        )
    }

    @Test
    fun testReadCurrentAttributesSourceIsNull() {
        val probeName = "APPLE01"
        val probeTestName = "ae2220.46_SBM-P-00000827-19_2"
        val id = IIDs.PROBES
            .child(Probe::class.java, ProbeKey(probeName))
            .child(ProbeTests::class.java)
            .child(ProbeTest::class.java, ProbeTestKey(probeTestName))
            .child(Config::class.java)

        val builder = ConfigBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.name, CoreMatchers.equalTo(probeTestName))
        Assert.assertThat(builder.source, CoreMatchers.`is`(CoreMatchers.nullValue()))
        Assert.assertThat(
            builder.getAugmentation(JuniperExtConfigAug::class.java),
            CoreMatchers.`is`(CoreMatchers.nullValue()))
    }

    @Test
    fun testMerge() {
        val config = Mockito.mock(Config::class.java)
        val parentBuilder = ProbeTestBuilder()

        target.merge(parentBuilder, config)

        Assert.assertThat(parentBuilder.config, CoreMatchers.sameInstance(config))
    }

    @Test
    fun testGetBuilder() {
        val result = target.getBuilder(IIDs.PR_PR_TE_TE_CONFIG)

        Assert.assertThat(result, CoreMatchers.instanceOf(ConfigBuilder::class.java))
    }
}