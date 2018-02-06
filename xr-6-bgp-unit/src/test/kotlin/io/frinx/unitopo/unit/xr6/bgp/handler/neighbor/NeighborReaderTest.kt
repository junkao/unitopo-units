package io.frinx.unitopo.unit.xr6.bgp.handler.neighbor

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress

class NeighborReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/bgp-conf2.xml")

    @Test
    fun testIds() {
        val parseNeighbors = NeighborReader.parseNeighbors(parseGetCfgResponse(DATA_NODES,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))), NetworkInstanceKey("default"));

        Assert.assertEquals(listOf("4004::1", "10.1.0.4", "2.3.4.5").map { NeighborKey(IpAddress(it.toCharArray())) }, parseNeighbors)

        val parseNeighborsVrf = NeighborReader.parseNeighbors(parseGetCfgResponse(DATA_NODES,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))), NetworkInstanceKey("abcd"));

        Assert.assertEquals(listOf("4444::1111").map { NeighborKey(IpAddress(it.toCharArray())) }, parseNeighborsVrf)

        val parseNeighborsEmpty = NeighborReader.parseNeighbors(parseGetCfgResponse(DATA_NODES,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))), NetworkInstanceKey("NONEXISTING"));

        Assert.assertEquals(emptyList<NeighborKey>(), parseNeighborsEmpty)
    }
}
