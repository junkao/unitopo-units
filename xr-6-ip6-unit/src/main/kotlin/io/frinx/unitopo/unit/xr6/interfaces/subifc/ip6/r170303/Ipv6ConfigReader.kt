package io.frinx.unitopo.unit.xr6.interfaces.subifc.ip6.r170303

import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.subifc.ip6.r150730.Ipv6ConfigReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.address.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303.InterfaceConfiguration1 as UnderlayIpv6Augment

class Ipv6ConfigReader(underlayAccess: UnderlayAccess) : Ipv6ConfigReader(underlayAccess) {

    override fun getHandler(builder: ConfigBuilder): (InterfaceConfiguration) -> kotlin.Unit =
            { extractAddress(it, builder) }

    companion object {
        private fun extractAddress(ifcCfg: InterfaceConfiguration, builder: ConfigBuilder) {
            // TODO this code is same as for the parent handler, since there is no real change between
            // 150730 revision of ipv6-cfg XR model and the 170303 model
            // The only difference is the yangtools generated type hierarchy and the code cant be reused
            // Think about making the parsing code dynamic to be available for multiple revisions
            ifcCfg.getAugmentation(UnderlayIpv6Augment::class.java)
                    ?.ipv6Network
                    ?.addresses
                    ?.let {
                        it.linkLocalAddress?.let {
                            if (builder.ip == it.address.ipv6AddressNoZone) {
                                builder.prefixLength = LINK_LOCAL_PREFIX
                            }
                        }
                        it.regularAddresses
                                ?.regularAddress.orEmpty()
                                .firstOrNull { builder.ip == it.address.ipv6AddressNoZone }
                                ?.let { builder.prefixLength = it.prefixLength.value.toShort() }
                    }
        }
    }
}