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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.neighbors.NeighborKey

class NeighborReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/lldp-oper.xml")

    @Test
    fun testAllIds() {
        val lldp = parseGetCfgResponse(DATA_NODES, InterfaceReader.LLDP_OPER)
        val neighbors = InterfaceReader.parseInterfaceNeighbors("GigabitEthernet0/0/0/3", lldp);

        Assert.assertEquals(
                listOf("PE2.demo.frinx.io")
                        .map { NeighborKey(it) },
                NeighborReader.parseDeviceIds(neighbors))
    }
}