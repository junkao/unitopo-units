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
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.Test
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.Probe
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.test.target.TargetBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.test.target.target.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.test.target.target.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.test.target.target.type.Case1
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.Probe as JunosProbe
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.ProbeKey as JunosProbeKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.Test as JunosTest
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.TestKey as JunosTestKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.test.Target as JunosTarget
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class ProbeTargetConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(instanceIdentifier: IID<Config>): ConfigBuilder = ConfigBuilder()

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val probeName = instanceIdentifier.firstKeyOf(Probe::class.java).name!!
        val testName = instanceIdentifier.firstKeyOf(Test::class.java).name!!

        val underlayId = getUnderlayId(probeName, testName)

        val junosTarget = underlayAccess.read(underlayId).checkedGet()

        if (junosTarget.isPresent) {
            configBuilder.fromUnderlay(junosTarget.get())
        }
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as TargetBuilder).config = config
    }

    companion object {
        fun getUnderlayId(probeName: String, testName: String): IID<JunosTarget> {
            return ProbeTestReader.JUNOS_RPM
                .child(JunosProbe::class.java, JunosProbeKey(probeName))
                .child(JunosTest::class.java, JunosTestKey(testName))
                .child(JunosTarget::class.java)
        }

        private fun ConfigBuilder.fromUnderlay(target: JunosTarget) {
            val targetType = target.targetType
            address = when (targetType) {
                is Case1 -> IpAddress(targetType.address.value.toCharArray())
                else -> null
            }
        }
    }
}