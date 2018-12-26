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
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.Probe
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.ProbeBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.probe.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.probe.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.juniper.rev181203.Config3 as JuniperExtConfigAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.juniper.rev181203.Config3Builder as JuniperExtConfigAugBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.Rpm as JunosRpm
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.Probe as JunosProbe
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.ProbeKey as JunosProbeKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class ProbeConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(instanceIdentifier: IID<Config>): ConfigBuilder = ConfigBuilder()

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val name = instanceIdentifier.firstKeyOf(Probe::class.java).name!!
        configBuilder.name = name
        val id = getUnderlayId(name)

        val junosProbe = underlayAccess.read(id).checkedGet()

        if (junosProbe.isPresent) {
            configBuilder.fromUnderlay(junosProbe.get())
        }
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as ProbeBuilder).config = config
    }

    companion object {
        fun getUnderlayId(name: String): IID<JunosProbe> {
            return ProbeReader.JUNOS_SERVICES
                .child(JunosRpm::class.java)
                .child(JunosProbe::class.java, JunosProbeKey(name))
        }

        private fun ConfigBuilder.fromUnderlay(probe: JunosProbe) {
            val juniperAugBuilder = JuniperExtConfigAugBuilder()

            juniperAugBuilder.fromUnderlay(probe)

            if (!juniperAugBuilder.isEmpty()) {
                addAugmentation(JuniperExtConfigAug::class.java, juniperAugBuilder.build())
            }
        }

        private fun JuniperExtConfigAugBuilder.fromUnderlay(probe: JunosProbe) {
            if (probe.isDelegateProbes != null) {
                isDelegateProbes = probe.isDelegateProbes
            }
        }

        private fun JuniperExtConfigAugBuilder.isEmpty(): Boolean {
            return when {
                isDelegateProbes != null -> false
                else -> true
            }
        }
    }
}