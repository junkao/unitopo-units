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

package io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.EnableDisable
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class SubinterfaceConfigReader(private val underlayAccess: UnderlayAccess)
    : ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(instanceIdentifier: IID<Config>): ConfigBuilder {
        return ConfigBuilder()
    }

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val name = instanceIdentifier.firstKeyOf(Interface::class.java).name
        val unitId = instanceIdentifier.firstKeyOf(Subinterface::class.java).index
        InterfaceReader.readUnitCfg(underlayAccess, name, unitId) { configBuilder.fromUnderlay(it) }
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as SubinterfaceBuilder).config = config
    }

    companion object {
        private fun parseEnableDisable(enableDisable: EnableDisable?): Boolean {
            return when (enableDisable) {
                null -> true
                else -> false
            }
        }

        internal fun ConfigBuilder.fromUnderlay(junosUnit: JunosInterfaceUnit) {
            index = junosUnit.name.toLong()
            isEnabled = parseEnableDisable(junosUnit.enableDisable)
        }
    }
}