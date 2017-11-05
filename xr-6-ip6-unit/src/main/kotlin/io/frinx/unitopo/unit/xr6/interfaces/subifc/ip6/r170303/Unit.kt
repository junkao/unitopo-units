package io.frinx.unitopo.unit.xr6.interfaces.subifc.ip6.r170303

import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.subifc.ip6.r150730.Unit
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.Subinterface2Builder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.Ipv6Builder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.AddressesBuilder

class Unit(registry: TranslationUnitCollector) : Unit(registry) {

    override fun getUnderlayYangSchemas() = setOf(
            org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303.`$YangModuleInfoImpl`.getInstance())

    override fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(SUBIFC_IPV6_AUG_ID, Subinterface2Builder::class.java)
        rRegistry.addStructuralReader(SUBIFC_IPV6_ID, Ipv6Builder::class.java)
        rRegistry.addStructuralReader(SUBIFC_IPV6_ADDRESSES_ID, AddressesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(SUBIFC_IPV6_ADDRESS_ID, Ipv6AddressReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(SUBIFC_IPV6_CFG_ID, Ipv6ConfigReader(underlayAccess)))
    }

    override fun toString(): String = "XR 6 (2017-03-03) IPv6 translate unit"
}