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

package io.frinx.unitopo.unit.junos18.probes.handler

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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.openconfig.probes.top.ProbesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.Probe
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.ProbeBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.ProbeKey

class ProbeReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private lateinit var target: ProbeReader

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        target = ProbeReader(underlayAccess)
    }

    @Test
    fun testGetAllIds() {
        val id = IIDs.PR_PROBE

        val result = target.getAllIds(id, readContext)

        Assert.assertThat(
            result.map { it.name },
            Matchers.containsInAnyOrder("MS00", "APPLE00", "APPLE01")
        )
    }

    @Test
    fun testReadCurrentAttributes() {
        val probeName = "MS00"
        val id = IIDs.PROBES.child(Probe::class.java, ProbeKey(probeName))
        val builder = ProbeBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.name, CoreMatchers.sameInstance(probeName))
    }

    @Test
    fun testMerge() {
        val parentBuilder = ProbesBuilder()
        val data: List<Probe> = emptyList()

        target.merge(parentBuilder, data)

        Assert.assertThat(parentBuilder.probe, CoreMatchers.sameInstance(data))
    }

    @Test
    fun testGetBuilder() {
        val result = target.getBuilder(IIDs.PR_PROBE)

        Assert.assertThat(result, CoreMatchers.instanceOf(ProbeBuilder::class.java))
    }
}