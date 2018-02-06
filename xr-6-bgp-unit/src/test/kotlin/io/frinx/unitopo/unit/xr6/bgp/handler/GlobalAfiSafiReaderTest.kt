package io.frinx.unitopo.unit.xr6.bgp.handler

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.IPV4UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.IPV6UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.L3VPNIPV4UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.L3VPNIPV6UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey

class GlobalAfiSafiReaderTest: AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/bgp-conf2.xml")

    @Test
    fun testGlobal() {
        val afiSafi = GlobalAfiSafiReader.parseAfiSafi(parseGetCfgResponse(DATA_NODES,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))), NetworkInstanceKey("default"))
        Assert.assertEquals(listOf(IPV4UNICAST::class.java, IPV6UNICAST::class.java, L3VPNIPV6UNICAST::class.java, L3VPNIPV4UNICAST::class.java)
                .map { AfiSafiKey(it) }, afiSafi)

        val afiSafiVrf = GlobalAfiSafiReader.parseAfiSafi(parseGetCfgResponse(DATA_NODES,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))), NetworkInstanceKey("abcd"))
        Assert.assertEquals(listOf(IPV4UNICAST::class.java).map { AfiSafiKey(it) }, afiSafiVrf)

        val afiSafiVrfEmpty = GlobalAfiSafiReader.parseAfiSafi(parseGetCfgResponse(DATA_NODES,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))), NetworkInstanceKey("NONEXISTING"))
        Assert.assertEquals(emptyList<AfiSafiKey>(), afiSafiVrfEmpty)
    }
}