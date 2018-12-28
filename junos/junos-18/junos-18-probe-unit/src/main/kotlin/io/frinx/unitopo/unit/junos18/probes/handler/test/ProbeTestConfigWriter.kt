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

import com.google.common.base.Optional
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.Test
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.test.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.Probe
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.common.types.rev180101.Ipv4addr
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.juniper.rev181203.Config4 as JuniperExtConfigAug
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.common.types.rev180101.InterfaceName as JunosTypesInterfaceName
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.Test as JunosTest
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.Test.DestinationInterface as JunosDestinationInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.TestBuilder as JunosTestBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.TestKey as JunosTestKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class ProbeTestConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {
    override fun writeCurrentAttributes(
        instanceIdentifier: IID<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val probeName = instanceIdentifier.firstKeyOf(Probe::class.java).name!!
        val testName = instanceIdentifier.firstKeyOf(Test::class.java).name!!
        val underlayId = ProbeTestConfigReader.getUnderlayId(probeName, testName)
        val builder = JunosTestBuilder()

        builder.fromOpenConfig(dataAfter)
        underlayAccess.put(underlayId, builder.build())
    }

    override fun updateCurrentAttributes(
        instanceIdentifier: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val probeName = instanceIdentifier.firstKeyOf(Probe::class.java).name!!
        val testName = instanceIdentifier.firstKeyOf(Test::class.java).name!!
        val underlayId = ProbeTestConfigReader.getUnderlayId(probeName, testName)
        val existingData = getExistingData(underlayId)
        val builder = when (existingData.isPresent) {
            true -> JunosTestBuilder(existingData.get())
            else -> JunosTestBuilder()
        }

        builder.fromOpenConfig(dataAfter)
        underlayAccess.put(underlayId, builder.build())
    }

    override fun deleteCurrentAttributes(
        instanceIdentifier: IID<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val probeName = instanceIdentifier.firstKeyOf(Probe::class.java).name!!
        val testName = instanceIdentifier.firstKeyOf(Test::class.java).name!!
        val underlayId = ProbeTestConfigReader.getUnderlayId(probeName, testName)

        underlayAccess.delete(underlayId)
    }

    private fun getExistingData(underlayId: IID<JunosTest>): Optional<JunosTest> {
        return underlayAccess.read(underlayId).checkedGet()
    }

    companion object {
        private fun JunosTestBuilder.fromOpenConfig(data: Config) {
            key = JunosTestKey(data.name)
            name = data.name

            val destInterface = data.getAugmentation(JuniperExtConfigAug::class.java)?.destinationInterface
            destinationInterface = when (destInterface) {
                null -> null
                else -> JunosDestinationInterface(JunosTypesInterfaceName(destInterface))
            }

            sourceAddress = data.source?.ipv4Address?.let { Ipv4addr(it.value) }
        }
    }
}