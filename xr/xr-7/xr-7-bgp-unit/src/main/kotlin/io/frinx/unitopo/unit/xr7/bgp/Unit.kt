/*
 * Copyright © 2019 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.bgp

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalAfiSafiConfigReader
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalAfiSafiConfigWriter
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalConfigReader
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalConfigWriter
import io.frinx.unitopo.unit.xr7.bgp.handler.GlobalAfiSafiReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.AfiSafisBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.BgpBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.GlobalBuilder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.`$YangModuleInfoImpl` as UnderlayIpv4BgpConfigYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.`$YangModuleInfoImpl` as BgpYangModule

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        if (reg != null) {
            reg!!.close()
        }
    }

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(
        BgpYangModule.getInstance()
    )

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
        UnderlayIpv4BgpConfigYangModule.getInstance()
    )

    override fun getRpcs(context: UnderlayAccess) = emptySet<RpcService<out DataObject, out DataObject>>()

    override fun provideHandlers(
        rRegistry: ModifiableReaderRegistryBuilder,
        wRegistry: ModifiableWriterRegistryBuilder,
        access: UnderlayAccess
    ) {
        provideReaders(rRegistry, access)
        provideWriters(wRegistry, access)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, access: UnderlayAccess) {
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, GlobalConfigWriter(access)),
            IIDs.NE_NE_CONFIG)

        wRegistry.add(GenericListWriter(IIDs.NE_NE_PR_PR_BG_GL_AF_AFISAFI, NoopListWriter()))

        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, GlobalAfiSafiConfigWriter(access)),
            IIDs.NE_NE_PR_PR_BG_GL_CONFIG)
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, access: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BGP, BgpBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_GLOBAL, GlobalBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_BG_GL_CONFIG, GlobalConfigReader(access)))
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_BG_GL_AFISAFIS, AfiSafisBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PR_BG_GL_AF_AFISAFI, GlobalAfiSafiReader(access)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, GlobalAfiSafiConfigReader(access)))
    }

    override fun toString() = "Translate unit for Cisco-IOS-XR-ipv4-bgp-cfg@2019-06-15"
}

typealias IID<T> = org.opendaylight.yangtools.yang.binding.InstanceIdentifier<T>

typealias UnderlayDefaultVrfGlobal = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.Global

typealias UnderlayBgp = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev180615.Bgp

typealias UnderlayBgpBuilder = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev180615.BgpBuilder

typealias UnderlayVrfGlobal = org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang
.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfGlobal