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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r170303

import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.fd.honeycomb.translate.util.write.NoopWriter
import io.frinx.openconfig.openconfig._if.ip.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r150730.Unit
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface2Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.Ipv6Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.AddressesBuilder

class Unit(registry: TranslationUnitCollector) : Unit(registry) {

    override fun getUnderlayYangSchemas() = setOf(
            org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco
                .ios.xr.ipv6.ma.cfg.rev170303.`$YangModuleInfoImpl`.getInstance())

    override fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE2_IP_AD_ADDRESS, NoopWriter()),
            setOf(IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE2_IPV6))
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE2_IP_AD_AD_CONFIG,
            Ipv6AddressConfigWriter(underlayAccess)), setOf(IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE2_IPV6,
            IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE2_IP_AD_ADDRESS))
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE2_IP_CONFIG,
            Ipv6ConfigWriter(underlayAccess)), IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE2_IPV6)
    }

    override fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE2, Subinterface2Builder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE2_IPV6, Ipv6Builder::class.java)
        rRegistry.addStructuralReader(IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE2_IP_ADDRESSES, AddressesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE2_IP_AD_ADDRESS,
            Ipv6AddressListReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE2_IP_AD_AD_CONFIG,
            Ipv6AddressConfigReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE2_IP_CONFIG,
            Ipv6ConfigReader(underlayAccess)))
    }

    override fun toString(): String = "XR 6 (2017-03-03) IPv6 translate unit"
}