package io.frinx.unitopo.unit.xr6.bgp.handler

import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.StateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP

class GlobalStateReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/bgp-default-oper.xml")
    private val DATA_NODES2 = getResourceAsString("/bgp-vrf-oper.xml")

    @Test
    fun testGlobal() {
        val sBuilder = StateBuilder()
        sBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES,
                GlobalStateReader.getId(ProtocolKey(BGP::class.java, "default"), NetworInstance.DEFAULT_NETWORK)))

        Assert.assertEquals(720919, sBuilder.`as`.value)
        Assert.assertEquals("1.1.1.1", sBuilder.routerId.value)
    }

    @Test
    fun testGlobalVrf() {
        val sBuilder = StateBuilder()
        sBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES2,
                GlobalStateReader.getId(ProtocolKey(BGP::class.java, "default"), NetworkInstanceKey("abcd"))))

        Assert.assertEquals(720919, sBuilder.`as`.value)
        Assert.assertEquals("8.8.8.8", sBuilder.routerId.value)
    }
}