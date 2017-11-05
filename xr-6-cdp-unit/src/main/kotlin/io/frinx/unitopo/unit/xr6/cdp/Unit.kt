/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.cdp

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.*
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.cdp.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.io.yang.cdp.rev171024.cdp.top.CdpBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp._interface.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.NeighborsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.cdp.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayCdpCfgYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.cdp.oper.rev150730.`$YangModuleInfoImpl` as UnderlayCdpOperYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.io.yang.cdp.rev171024.`$YangModuleInfoImpl` as CdpYangInfo
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

    override fun getYangSchemas() = setOf(CdpYangInfo.getInstance())

    override fun getUnderlayYangSchemas() = setOf(
            UnderlayCdpCfgYangInfo.getInstance(),
            UnderlayCdpOperYangInfo.getInstance())

    override fun getRpcs(underlayAccess: UnderlayAccess) = emptySet<RpcService<*, *>>()

    override fun provideHandlers(rRegistry: ModifiableReaderRegistryBuilder,
                                 wRegistry: ModifiableWriterRegistryBuilder,
                                 underlayAccess: UnderlayAccess) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {

    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.CDP, CdpBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.CD_INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.CD_IN_INTERFACE, InterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.CD_IN_IN_CONFIG, InterfaceConfigReader()))
        rRegistry.add(GenericOperReader(IIDs.CD_IN_IN_STATE, InterfaceStateReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.CD_IN_IN_NEIGHBORS, NeighborsBuilder::class.java)
        rRegistry.add(GenericOperListReader(IIDs.CD_IN_IN_NE_NEIGHBOR, NeighborReader(underlayAccess)))
        rRegistry.add(GenericOperReader(IIDs.CD_IN_IN_NE_NE_STATE, NeighborStateReader(underlayAccess)))
    }

    override fun toString(): String = "XR 6 (2015-07-30) CDP translate unit"
}


