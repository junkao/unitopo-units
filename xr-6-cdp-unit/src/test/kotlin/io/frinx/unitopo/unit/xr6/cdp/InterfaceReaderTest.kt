package io.frinx.unitopo.unit.xr6.cdp

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.InterfaceKey

class InterfaceReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/cdp-oper.xml")

    @Test
    fun testAllIds() {
        Assert.assertEquals(
                listOf("GigabitEthernet0/0/0/3", "MgmtEth0/0/CPU0/0", "GigabitEthernet0/0/0/2")
                        .map { InterfaceKey(it) },
                InterfaceReader.parseInterfaceIds(
                        InterfaceReader.parseInterfaces(parseGetCfgResponse(DATA_NODES, InterfaceReader.CDP_OPER))))
    }
}