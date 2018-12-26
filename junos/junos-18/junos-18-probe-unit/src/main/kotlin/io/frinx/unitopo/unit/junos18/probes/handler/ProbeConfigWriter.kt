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

import com.google.common.base.Optional
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.Probe
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.probe.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.juniper.rev181203.Config3 as JuniperExtConfigAug
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.Probe as JunosProbe
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.ProbeBuilder as JunosProbeBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.ProbeKey as JunosProbeKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class ProbeConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {
    override fun writeCurrentAttributes(
        instanceIdentifier: IID<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val name = instanceIdentifier.firstKeyOf(Probe::class.java).name!!
        val underlayId = ProbeConfigReader.getUnderlayId(name)
        val builder = JunosProbeBuilder()

        builder.fromOpenConfig(dataAfter)
        underlayAccess.put(underlayId, builder.build())
    }

    override fun updateCurrentAttributes(
        instanceIdentifier: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val name = instanceIdentifier.firstKeyOf(Probe::class.java).name!!
        val underlayId = ProbeConfigReader.getUnderlayId(name)
        val existingData = getExistingData(underlayId)

        val builder = when (existingData.isPresent) {
            true -> JunosProbeBuilder(existingData.get())
            else -> JunosProbeBuilder()
        }

        builder.fromOpenConfig(dataAfter)
        underlayAccess.put(underlayId, builder.build())
    }

    override fun deleteCurrentAttributes(
        instanceIdentifier: IID<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val name = instanceIdentifier.firstKeyOf(Probe::class.java).name!!
        val underlayId = ProbeConfigReader.getUnderlayId(name)

        underlayAccess.delete(underlayId)
    }

    private fun getExistingData(underlayId: IID<JunosProbe>): Optional<JunosProbe> {
        return underlayAccess.read(underlayId).checkedGet()
    }

    companion object {
        private fun JunosProbeBuilder.fromOpenConfig(data: Config) {
            key = JunosProbeKey(data.name)

            name = data.name
            isDelegateProbes = data.getAugmentation(JuniperExtConfigAug::class.java)?.isDelegateProbes
        }
    }
}