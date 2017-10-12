/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.interfaces

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericListReader
import io.fd.honeycomb.translate.impl.read.GenericReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.subifc.*
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.Subinterface1
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.Subinterface1Builder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.Subinterface2
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.Subinterface2Builder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.Ipv4
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.Ipv4Builder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.Addresses
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.AddressesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.Address
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.Ipv6
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.Ipv6Builder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730.`$YangModuleInfoImpl` as UnderlayInterfacesOperYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayIpv4YangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayIpv6YangInfo
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.`$YangModuleInfoImpl` as IpYangInfo
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.address.Config as Ipv4AddressConfig
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.Addresses as Ipv6Addresses
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.AddressesBuilder as Ipv6AddressesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.Address as Ipv6Address
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.address.Config as Ipv6AddressConfig
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.`$YangModuleInfoImpl` as InterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.Config as SubinterfaceConfig
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.State as SubinterfaceState

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(
            InterfacesYangInfo.getInstance(),
            IpYangInfo.getInstance())

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
            UnderlayInterfacesYangInfo.getInstance(),
            UnderlayInterfacesOperYangInfo.getInstance(),
            UnderlayIpv4YangInfo.getInstance(),
            UnderlayIpv6YangInfo.getInstance())

    override fun getRpcs(underlayAccess: UnderlayAccess): Set<RpcService<*, *>> = emptySet()

    override fun provideHandlers(rRegistry: ModifiableReaderRegistryBuilder,
                                 wRegistry: ModifiableWriterRegistryBuilder,
                                 underlayAccess: UnderlayAccess) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        // TODO extract noop writer and use that, then delete empty InterfaceWriter
        wRegistry.add(GenericListWriter(IIDs.IN_INTERFACE, InterfaceWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_CONFIG, InterfaceConfigWriter(underlayAccess)))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericListReader(IIDs.IN_INTERFACE, InterfaceReader(underlayAccess)))
        rRegistry.add(GenericReader(IIDs.IN_IN_STATE, InterfaceStateReader(underlayAccess)))
        rRegistry.add(GenericReader(IIDs.IN_IN_CONFIG, InterfaceConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IIDs.IN_IN_SUBINTERFACES, SubinterfacesBuilder::class.java)
        rRegistry.add(GenericListReader(IIDs.IN_IN_SU_SUBINTERFACE, SubinterfaceReader(underlayAccess)))

        rRegistry.addStructuralReader(SUBIFC_IPV4_AUG_ID, Subinterface1Builder::class.java)
        rRegistry.addStructuralReader(SUBIFC_IPV4_ID, Ipv4Builder::class.java)
        rRegistry.addStructuralReader(SUBIFC_IPV4_ADDRESSES_ID, AddressesBuilder::class.java)
        rRegistry.add(GenericListReader(SUBIFC_IPV4_ADDRESS_ID, Ipv4AddressReader(underlayAccess)))
        rRegistry.add(GenericReader(SUBIFC_IPV4_CFG_ID, Ipv4ConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(SUBIFC_IPV6_AUG_ID, Subinterface2Builder::class.java)
        rRegistry.addStructuralReader(SUBIFC_IPV6_ID, Ipv6Builder::class.java)
        rRegistry.addStructuralReader(SUBIFC_IPV6_ADDRESSES_ID, Ipv6AddressesBuilder::class.java)
        rRegistry.add(GenericListReader(SUBIFC_IPV6_ADDRESS_ID, Ipv6AddressReader(underlayAccess)))
        rRegistry.add(GenericReader(SUBIFC_IPV6_CFG_ID, Ipv6ConfigReader(underlayAccess)))
    }

    override fun toString(): String = "XR 6 (2015-07-30) interface translate unit"

    companion object {
        private val SUBIFC_IPV4_AUG_ID = IIDs.IN_IN_SU_SUBINTERFACE.augmentation(Subinterface1::class.java)
        private val SUBIFC_IPV6_AUG_ID = IIDs.IN_IN_SU_SUBINTERFACE.augmentation(Subinterface2::class.java)

        private val SUBIFC_IPV4_ID = SUBIFC_IPV4_AUG_ID.child(Ipv4::class.java)
        private val SUBIFC_IPV4_ADDRESSES_ID = SUBIFC_IPV4_ID.child(Addresses::class.java)
        private val SUBIFC_IPV4_ADDRESS_ID = SUBIFC_IPV4_ADDRESSES_ID.child(Address::class.java)
        private val SUBIFC_IPV4_CFG_ID = SUBIFC_IPV4_ADDRESS_ID.child(Ipv4AddressConfig::class.java)

        private val SUBIFC_IPV6_ID = SUBIFC_IPV6_AUG_ID.child(Ipv6::class.java)
        private val SUBIFC_IPV6_ADDRESSES_ID = SUBIFC_IPV6_ID.child(Ipv6Addresses::class.java)
        private val SUBIFC_IPV6_ADDRESS_ID = SUBIFC_IPV6_ADDRESSES_ID.child(Ipv6Address::class.java)
        private val SUBIFC_IPV6_CFG_ID = SUBIFC_IPV6_ADDRESS_ID.child(Ipv6AddressConfig::class.java)
    }
}


