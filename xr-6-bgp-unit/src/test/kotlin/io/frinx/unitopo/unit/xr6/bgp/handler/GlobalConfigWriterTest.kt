package io.frinx.unitopo.unit.xr6.bgp.handler

import io.frinx.unitopo.unit.xr6.bgp.UnderlayRouteDistinguisherBuilder
import org.junit.Assert.assertEquals
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.BgpRouteDistinguisher
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpExtcommAsnIndex
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpExtcommV4AddrIndex
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.RouteDistinguisher
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone

class GlobalConfigWriterTest {

    @Test
    fun testRd() {
        assertEquals(
                UnderlayRouteDistinguisherBuilder()
                        .setType(BgpRouteDistinguisher.As)
                        .setAsXx(BgpAsRange(0))
                        .setAs(BgpAsRange(1))
                        .setAsIndex(BgpExtcommAsnIndex(2))
                        .build(),
                RouteDistinguisher("1:2").toXrRouteDistinguisher())

        assertEquals(
                UnderlayRouteDistinguisherBuilder()
                        .setType(BgpRouteDistinguisher.FourByteAs)
                        .setAsXx(BgpAsRange(1))
                        .setAs(BgpAsRange(34464))
                        .setAsIndex(BgpExtcommAsnIndex(22))
                        .build(),
                RouteDistinguisher("100000:22").toXrRouteDistinguisher())

        assertEquals(
                UnderlayRouteDistinguisherBuilder()
                        .setType(BgpRouteDistinguisher.Ipv4Address)
                        .setAddress(Ipv4AddressNoZone("2.11.240.1"))
                        .setAddressIndex(BgpExtcommV4AddrIndex(223))
                        .build(),
                RouteDistinguisher("2.11.240.1:223").toXrRouteDistinguisher())


    }

    @Test(expected = IllegalArgumentException::class)
    fun testRdIllegal() {
        RouteDistinguisher("4r398453245987;'.';2;34'.;2';").toXrRouteDistinguisher()
    }
}