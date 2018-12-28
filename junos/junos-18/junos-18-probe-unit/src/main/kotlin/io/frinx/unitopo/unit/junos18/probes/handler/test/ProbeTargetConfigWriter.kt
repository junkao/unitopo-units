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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.Probe
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.test.target.target.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.common.types.rev180101.Ipv4addr
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.test.target.target.type.Case1Builder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.test.Target as JunosTarget
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.test.TargetBuilder as JunosTargetBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class ProbeTargetConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {
    override fun writeCurrentAttributes(
        instanceIdentifier: IID<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val probeName = instanceIdentifier.firstKeyOf(Probe::class.java).name!!
        val testName = instanceIdentifier.firstKeyOf(Test::class.java).name!!
        val underlayId = ProbeTargetConfigReader.getUnderlayId(probeName, testName)
        val builder = JunosTargetBuilder()

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
        val underlayId = ProbeTargetConfigReader.getUnderlayId(probeName, testName)
        val existingData = getExistingData(underlayId)

        val builder = when (existingData.isPresent) {
            true -> JunosTargetBuilder(existingData.get())
            else -> JunosTargetBuilder()
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
        val underlayId = ProbeTargetConfigReader.getUnderlayId(probeName, testName)

        underlayAccess.delete(underlayId)
    }

    private fun getExistingData(underlayId: IID<JunosTarget>): Optional<JunosTarget> {
        return underlayAccess.read(underlayId).checkedGet()
    }

    companion object {
        private fun JunosTargetBuilder.fromOpenConfig(data: Config) {
            val address = data.address?.value?.let { String(it) }
            targetType = when {
                data.address?.ipv4Address != null -> Case1Builder()
                    .setAddress(Ipv4addr(address))
                    .build()
                else -> null
            }
        }
    }
}