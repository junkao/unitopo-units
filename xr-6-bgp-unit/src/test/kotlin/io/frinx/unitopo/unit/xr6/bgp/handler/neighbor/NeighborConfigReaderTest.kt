package io.frinx.unitopo.unit.xr6.bgp.handler.neighbor

import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv4Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv6Address

class NeighborConfigReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/bgp-conf2.xml")

    @Test
    fun testIds() {
        val instance = parseGetCfgResponse(DATA_NODES,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default"))))
        val builder = ConfigBuilder()

        NeighborConfigReader.parseNeighbor(instance,
                NetworInstance.DEFAULT_NETWORK,
                NeighborKey(IpAddress(Ipv4Address("10.1.0.4"))),
                builder)

        Assert.assertEquals(ConfigBuilder()
                .setEnabled(true)
                .setNeighborAddress(IpAddress(Ipv4Address("10.1.0.4")))
                .setPeerAs(AsNumber(123))
                .build(),
                builder.build())
    }

    @Test
    fun testIdsVrf() {
        val instance = parseGetCfgResponse(DATA_NODES,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default"))))
        val builder = ConfigBuilder()

        NeighborConfigReader.parseNeighbor(instance,
                NetworkInstanceKey("abcd"),
                NeighborKey(IpAddress(Ipv6Address("4444::1111"))),
                builder)

        Assert.assertEquals(ConfigBuilder()
                .setEnabled(true)
                .setNeighborAddress(IpAddress(Ipv6Address("4444::1111")))
                .setPeerAs(AsNumber(65537))
                .build(),
                builder.build())
    }

    @Test
    fun testNoData() {
        val builder = ConfigBuilder()

        NeighborConfigReader.parseNeighbor(null,
                NetworkInstanceKey("abcd"),
                NeighborKey(IpAddress(Ipv6Address("4444::1111"))),
                builder)

        Assert.assertEquals(ConfigBuilder().build(), builder.build())
    }
}