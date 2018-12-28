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
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.probes.handler.ProbeReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.Test
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.TestBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.TestKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.Probe
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.probe.TestsBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.Rpm as JunosRpm
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.Probe as JunosProbe
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.ProbeKey as JunosProbeKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class ProbeTestReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Test, TestKey, TestBuilder> {

    override fun getAllIds(instanceIdentifier: IID<Test>, readContext: ReadContext): List<TestKey> {
        val probeName = instanceIdentifier.firstKeyOf(Probe::class.java).name
        return getTestIds(underlayAccess, probeName)
    }

    override fun merge(builder: Builder<out DataObject>, list: List<Test>) {
        (builder as TestsBuilder).test = list
    }

    override fun getBuilder(instanceIdentifier: IID<Test>): TestBuilder = TestBuilder()

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Test>,
        testBuilder: TestBuilder,
        readContext: ReadContext
    ) {
        val testName = instanceIdentifier.firstKeyOf(Test::class.java).name
        testBuilder.name = testName
    }

    companion object {
        val JUNOS_RPM = ProbeReader.JUNOS_SERVICES
            .child(JunosRpm::class.java)

        private fun getTestIds(underlayAccess: UnderlayAccess, probeName: String): List<TestKey> {
            val underlayId = getUnderlayId(probeName)
            return underlayAccess.read(underlayId, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()
                ?.let { probe ->
                    probe.test.orEmpty()
                        .map { TestKey(it.name) }
                }.orEmpty()
        }

        private fun getUnderlayId(probeName: String): IID<JunosProbe> {
            return JUNOS_RPM.child(JunosProbe::class.java, JunosProbeKey(probeName))
        }
    }
}