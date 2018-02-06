package io.frinx.unitopo.unit.xr6.bgp.handler.neighbor

import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.StateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv4Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv6Address

class NeighborStateReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/bgp-neighbor-global-oper.xml")
    private val DATA_NODES2 = getResourceAsString("/bgp-neighbor-vrf-oper.xml")

    @Test
    fun testIds() {
        val stateBuilder = StateBuilder()
        stateBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES,
                NeighborStateReader.getId(
                        ProtocolKey(BGP::class.java, "default"),
                        NetworInstance.DEFAULT_NETWORK,
                        NeighborKey(IpAddress(Ipv4Address("10.1.0.4"))))))

        Assert.assertEquals(
                StateBuilder()
                        .setEnabled(true)
                        .setNeighborAddress(IpAddress(Ipv4Address("10.1.0.4")))
                        .setPeerAs(AsNumber(123))
                        .build(),
                stateBuilder.build())
    }

    @Test
    fun testIdsVrf() {
        val stateBuilder = StateBuilder()
        stateBuilder.fromUnderlay(parseGetCfgResponse(DATA_NODES2,
                NeighborStateReader.getId(
                        ProtocolKey(BGP::class.java, "default"),
                        NetworkInstanceKey("abcd"),
                        NeighborKey(IpAddress(Ipv6Address("4444::1111"))))))

        Assert.assertEquals(
                StateBuilder()
                        .setEnabled(true)
                        .setNeighborAddress(IpAddress(Ipv6Address("4444::1111")))
                        .setPeerAs(AsNumber(65537))
                        .build(),
                stateBuilder.build())
    }

    @Test
    fun testNoData() {
        val stateBuilder = StateBuilder()
        stateBuilder.fromUnderlay(null)
        Assert.assertEquals(StateBuilder().build(), stateBuilder.build())
    }
}