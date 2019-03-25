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

package io.frinx.unitopo.unit.xr6.cdp

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericOperListReader
import io.fd.honeycomb.translate.impl.read.GenericOperReader
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.cdp.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.init.Unit
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.cdp.rev171024.cdp.top.CdpBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp._interface.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.NeighborsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.cdp.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayCdpCfgYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.cdp.oper.rev150730.`$YangModuleInfoImpl` as UnderlayCdpOperYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.cdp.rev171024.`$YangModuleInfoImpl` as CdpYangInfo

class Unit(private val registry: TranslationUnitCollector) : Unit() {
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

    override fun provideHandlers(
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, underlayAccess: UnderlayAccess) {
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.CDP, CdpBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.CD_INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericOperListReader(IIDs.CD_IN_INTERFACE, InterfaceReader(underlayAccess)))
        rRegistry.add(GenericOperReader(IIDs.CD_IN_IN_CONFIG, InterfaceConfigReader()))
        rRegistry.addStructuralReader(IIDs.CD_IN_IN_NEIGHBORS, NeighborsBuilder::class.java)
        rRegistry.add(GenericOperListReader(IIDs.CD_IN_IN_NE_NEIGHBOR, NeighborReader(underlayAccess)))
        rRegistry.add(GenericOperReader(IIDs.CD_IN_IN_NE_NE_STATE, NeighborStateReader(underlayAccess)))
    }

    override fun toString(): String = "XR 6 (2015-07-30) CDP translate unit"
}