/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lldp

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.InterfaceKey

class InterfaceReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/lldp-oper.xml")

    @Test
    fun testAllIds() {
        Assert.assertEquals(
                listOf("GigabitEthernet0/0/0/3", "GigabitEthernet0/0/0/2")
                        .map { InterfaceKey(it) },
                InterfaceReader.parseInterfaceIds(
                        InterfaceReader.parseInterfaces(parseGetCfgResponse(DATA_NODES, InterfaceReader.LLDP_OPER))))
    }
}