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

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.event.types.rev171024.LINKUPDOWN
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp._interface.config.EnabledTrapForEventBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces.InterfaceKey as SnmpInterfaceKey

class SnmpInterfaceConfigReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/interfaces-conf.xml")

    @Test
    fun testGlobal() {
        val ifcs = SnmpInterfaceReader.parseInterfaceIds(parseGetCfgResponse(DATA_NODES,
                SnmpInterfaceReader.UNDERLAY_IFC_ID))
        ifcs.toMap()

        var builder = ConfigBuilder()
        var key = SnmpInterfaceKey(InterfaceId("ge-0/0/0"))
        SnmpConfigReader.parseCurrentAttributes(ifcs.toMap(), key, builder)
        assertEquals(getConfig(key, false), builder.build())

        builder = ConfigBuilder()
        key = SnmpInterfaceKey(InterfaceId("ge-0/0/1"))
        SnmpConfigReader.parseCurrentAttributes(ifcs.toMap(), key, builder)
        assertEquals(getConfig(key, true), builder.build())

        builder = ConfigBuilder()
        key = SnmpInterfaceKey(InterfaceId("ge-0/0/9999"))
        SnmpConfigReader.parseCurrentAttributes(ifcs.toMap(), key, builder)
        assertEquals(ConfigBuilder().build(), builder.build())
    }

    private fun getConfig(key: SnmpInterfaceKey, enabled: Boolean): Config? {
        return ConfigBuilder()
                .setInterfaceId(key.interfaceId)
                .setEnabledTrapForEvent(listOf(EnabledTrapForEventBuilder()
                        .setEventName(LINKUPDOWN::class.java)
                        .setEnabled(enabled)
                        .build()))
                .build()
    }
}