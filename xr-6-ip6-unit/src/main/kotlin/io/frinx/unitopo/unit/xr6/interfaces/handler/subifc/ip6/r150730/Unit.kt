/*
 * Copyright © 2018 Frinx and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r150730

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface2
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface2Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.Ipv6
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.Ipv6Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.Addresses
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.AddressesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.address.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.`$YangModuleInfoImpl`
import io.frinx.openconfig.openconfig.network.instance.IIDs as NetworkInstanceIIDs

open class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas() = setOf(
            `$YangModuleInfoImpl`.getInstance(),
            org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang
                .interfaces.ip.rev161222.`$YangModuleInfoImpl`.getInstance())

    override fun getUnderlayYangSchemas() = setOf(
            org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios
                .xr.ipv6.ma.cfg.rev150730.`$YangModuleInfoImpl`.getInstance())

    override fun getRpcs(underlayAccess: UnderlayAccess) = emptySet<RpcService<*, *>>()

    override fun provideHandlers(
        rRegistry: ModifiableReaderRegistryBuilder,
        wRegistry: ModifiableWriterRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    open fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.add(GenericWriter(SUBIFC_IPV6_ADDRESS_ID, Ipv6AddressWriter()))
        wRegistry.addAfter(GenericWriter(SUBIFC_IPV6_CFG_ID, Ipv6ConfigWriter(underlayAccess)),
                NetworkInstanceIIDs.NE_NE_IN_IN_CONFIG)
    }

    open fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(SUBIFC_IPV6_AUG_ID, Subinterface2Builder::class.java)
        rRegistry.addStructuralReader(SUBIFC_IPV6_ID, Ipv6Builder::class.java)
        rRegistry.addStructuralReader(SUBIFC_IPV6_ADDRESSES_ID, AddressesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(SUBIFC_IPV6_ADDRESS_ID, Ipv6AddressReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(SUBIFC_IPV6_CFG_ID, Ipv6ConfigReader(underlayAccess)))
    }

    override fun toString(): String = "XR 6 (2015-07-30) IPv6 translate unit"

    companion object {
        val SUBIFC_IPV6_AUG_ID = IIDs.IN_IN_SU_SUBINTERFACE.augmentation(Subinterface2::class.java)!!

        val SUBIFC_IPV6_ID = SUBIFC_IPV6_AUG_ID.child(Ipv6::class.java)!!
        val SUBIFC_IPV6_ADDRESSES_ID = SUBIFC_IPV6_ID.child(Addresses::class.java)!!
        val SUBIFC_IPV6_ADDRESS_ID = SUBIFC_IPV6_ADDRESSES_ID.child(Address::class.java)!!
        val SUBIFC_IPV6_CFG_ID = SUBIFC_IPV6_ADDRESS_ID.child(Config::class.java)!!
    }
}