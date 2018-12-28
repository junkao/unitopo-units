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
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.Probe
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.ProbeKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.Test as ProbeTest
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.TestBuilder as ProbeTestBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.TestKey as ProbeTestKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.probe.Tests as ProbeTests
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.probe.TestsBuilder as ProbeTestsBuilder

class ProbeTestReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private lateinit var target: ProbeTestReader

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        target = ProbeTestReader(underlayAccess)
    }

    @Test
    fun testGetAllIds() {
        val probeName = "MS00"
        val id = IIDs.PROBES
                .child(Probe::class.java, ProbeKey(probeName))
                .child(ProbeTests::class.java)
                .child(ProbeTest::class.java)

        val result = target.getAllIds(id, readContext)

        Assert.assertThat(
                result.map { it.name },
                Matchers.containsInAnyOrder("ae2220.46_SBM-P-00000827-19", "ae2220.46_SBM-P-00000827-19_1")
        )
    }

    @Test
    fun testReadCurrentAttributes() {
        val probeName = "MS00"
        val probeTestName = "ae2220.46_SBM-P-00000827-19"
        val id = IIDs.PROBES
                .child(Probe::class.java, ProbeKey(probeName))
                .child(ProbeTests::class.java)
                .child(ProbeTest::class.java, ProbeTestKey(probeTestName))
        val builder = ProbeTestBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.name, CoreMatchers.sameInstance(probeTestName))
    }

    @Test
    fun testMerge() {
        val parentBuilder = ProbeTestsBuilder()
        val data: List<ProbeTest> = emptyList()

        target.merge(parentBuilder, data)

        Assert.assertThat(parentBuilder.test, CoreMatchers.sameInstance(data))
    }

    @Test
    fun testGetBuilder() {
        val result = target.getBuilder(IIDs.PR_PR_TE_TEST)

        Assert.assertThat(result, CoreMatchers.instanceOf(ProbeTestBuilder::class.java))
    }
}