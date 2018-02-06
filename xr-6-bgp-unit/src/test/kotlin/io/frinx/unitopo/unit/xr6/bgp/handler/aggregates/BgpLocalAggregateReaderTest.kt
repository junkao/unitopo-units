package io.frinx.unitopo.unit.xr6.bgp.handler.aggregates

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpPrefix

class BgpLocalBgpLocalAggregateReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/bgp-conf2.xml")

    @Test
    fun testIds() {
        val parseAggregates = BgpLocalAggregateReader.parseAggregates(parseGetCfgResponse(DATA_NODES,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))), NetworkInstanceKey("default"));

        Assert.assertEquals(listOf("42.41.43.0/24", "9009::/64").map { AggregateKey(IpPrefix(it.toCharArray())) }, parseAggregates)

        val parseAggregatesVrf = BgpLocalAggregateReader.parseAggregates(parseGetCfgResponse(DATA_NODES,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))), NetworkInstanceKey("abcd"));

        Assert.assertEquals(listOf("1.2.0.0/16").map { AggregateKey(IpPrefix(it.toCharArray())) }, parseAggregatesVrf)

        val parseAggregatesEmpty = BgpLocalAggregateReader.parseAggregates(parseGetCfgResponse(DATA_NODES,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))), NetworkInstanceKey("NONEXISTING"));

        Assert.assertEquals(emptyList<AggregateKey>(), parseAggregatesEmpty)
    }

}