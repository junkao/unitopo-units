package io.frinx.unitopo.unit.xr6.interfaces

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey

class InterfaceReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/data_nodes.xml")

    @Ignore
    @Test
    fun testAllIds() {
        Assert.assertEquals(
                listOf("Null0", "GigabitEthernet0/0/0/0", "GigabitEthernet0/0/0/1", "GigabitEthernet0/0/0/2",
                        "GigabitEthernet0/0/0/3", "GigabitEthernet0/0/0/4", "GigabitEthernet0/0/0/5", "FINT0/0/CPU0",
                        "Loopback0", "nV-Loopback0", "nV-Loopback1", "Null0", "MgmtEth0/0/CPU0/0")
                        .map { InterfaceKey(it) }
                        .toSet(),
                InterfaceReader.parseInterfaceIds(parseGetCfgResponse(DATA_NODES, InterfaceReader.DATA_NODES_ID))
                        .toSet())
    }
}