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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces.InterfaceKey as SnmpInterfaceKey

class SnmpInterfaceReaderTest : AbstractNetconfHandlerTest() {

    val DATA_NODES = getResourceAsString("/interfaces-conf.xml")

    @Test
    fun testGlobal() {
        val ifcs = SnmpInterfaceReader.parseInterfaceIds(parseGetCfgResponse(DATA_NODES,
            SnmpInterfaceReader.UNDERLAY_IFC_ID))
        assertEquals(listOf("ae656" to true, "ge-0/0/0" to false, "ge-0/0/1" to true)
            .map { SnmpInterfaceKey(InterfaceId(it.first)) to it.second }
            .toList(),
            ifcs.sortedBy { it.first.interfaceId.value })
    }
}