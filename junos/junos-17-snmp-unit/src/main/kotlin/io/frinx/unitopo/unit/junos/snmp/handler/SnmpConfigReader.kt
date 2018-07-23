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

package io.frinx.unitopo.unit.junos.snmp.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.event.types.rev171024.LINKUPDOWN
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp._interface.config.EnabledTrapForEventBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces._interface.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces.Interface as SnmpInterface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class SnmpConfigReader(private val underlayAccess: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(id: IID<Config>): ConfigBuilder = ConfigBuilder()

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as InterfaceBuilder).config = config
    }

    override fun readCurrentAttributes(id: IID<Config>, config: ConfigBuilder, readContext: ReadContext) {
        val ifcs = SnmpInterfaceReader.readInterfaceIds(underlayAccess).toMap()
        val key = id.firstKeyOf(SnmpInterface::class.java)
        parseCurrentAttributes(ifcs, key, config)
    }

    companion object {
        fun parseCurrentAttributes(ifcs: Map<InterfaceKey, Boolean>, key: InterfaceKey, config: ConfigBuilder) {
            if (ifcs.containsKey(key)) {
                config.interfaceId = InterfaceId(key.interfaceId)
                config.enabledTrapForEvent = listOf(EnabledTrapForEventBuilder()
                        .setEventName(LINKUPDOWN::class.java)
                        .setEnabled(ifcs[key])
                        .build())
            }
        }
    }
}