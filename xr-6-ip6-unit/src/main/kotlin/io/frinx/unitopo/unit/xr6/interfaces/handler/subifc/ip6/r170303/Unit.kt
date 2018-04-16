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
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r150730.Ipv6AddressWriter
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r150730.Unit
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface2Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.Ipv6Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.AddressesBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs as NetworkInstanceIIDs

class Unit(registry: TranslationUnitCollector) : Unit(registry) {

    override fun getUnderlayYangSchemas() = setOf(
            org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303.`$YangModuleInfoImpl`.getInstance())

    override fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.add(GenericWriter(SUBIFC_IPV6_ADDRESS_ID, Ipv6AddressWriter()))
        wRegistry.addAfter(GenericWriter(SUBIFC_IPV6_CFG_ID, Ipv6ConfigWriter(underlayAccess)),
                NetworkInstanceIIDs.NE_NE_IN_IN_CONFIG)
    }

    override fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(SUBIFC_IPV6_AUG_ID, Subinterface2Builder::class.java)
        rRegistry.addStructuralReader(SUBIFC_IPV6_ID, Ipv6Builder::class.java)
        rRegistry.addStructuralReader(SUBIFC_IPV6_ADDRESSES_ID, AddressesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(SUBIFC_IPV6_ADDRESS_ID, Ipv6AddressReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(SUBIFC_IPV6_CFG_ID, Ipv6ConfigReader(underlayAccess)))
    }

    override fun toString(): String = "XR 6 (2017-03-03) IPv6 translate unit"
}