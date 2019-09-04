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
package io.frinx.unitopo.unit.xr66.isis

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr66.isis.handler.global.IsisGlobalAfListReader
import io.frinx.unitopo.unit.xr66.isis.handler.global.IsisGlobalAfListWriter
import io.frinx.unitopo.unit.xr66.isis.handler.global.IsisGlobalConfigReader
import io.frinx.unitopo.unit.xr66.isis.handler.global.IsisGlobalConfigWriter
import io.frinx.unitopo.unit.xr66.isis.handler.global.IsisRedistributionConfigReader
import io.frinx.unitopo.unit.xr66.isis.handler.global.IsisRedistributionListReader
import io.frinx.unitopo.unit.xr66.isis.handler.global.IsisRedistributionListWriter
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.`$YangModuleInfoImpl` as UnderlayIsisConfigYangModule
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev170501.`$YangModuleInfoImpl` as UnderlayIsisTypesYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.`$YangModuleInfoImpl` as OpenconfigIsisExtensionModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.`$YangModuleInfoImpl` as OpenconfigIsisYangModule

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

    override fun getYangSchemas() = setOf(OpenconfigIsisYangModule.getInstance(),
            OpenconfigIsisExtensionModule.getInstance())

    override fun getUnderlayYangSchemas() = setOf(UnderlayIsisConfigYangModule.getInstance(),
            UnderlayIsisTypesYangModule.getInstance())

    override fun getRpcs(context: UnderlayAccess) = emptySet<RpcService<out DataObject, out DataObject>>()

    override fun provideHandlers(
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, access: UnderlayAccess) {
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_ISIS)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_IS_GLOBAL)
        wRegistry.subtreeAdd(IIDs.NE_NE_PR_PR_IS_GL_CONFIG,
            IsisGlobalConfigWriter(access),
            setOf(IIDs.NE_NE_PR_PR_IS_GL_CO_AUG_ISISGLOBALCONFAUG))
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_IS_GL_AFISAFI)
        wRegistry.subtreeAdd(IIDs.NE_NE_PR_PR_IS_GL_AF_AF,
            IsisGlobalAfListWriter(access),
            setOf(IIDs.NE_NE_PR_PR_IS_GL_AF_AF_CONFIG)
        )
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_IS_GL_AF_AF_AUG_ISISGLOBALAFISAFICONFAUG)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_IS_GL_AF_AF_AUG_ISISGLOBALAFISAFICONFAUG_REDISTRIBUTIONS)
        wRegistry.subtreeAdd(IIDs.NE_NE_PR_PR_IS_GL_AF_AF_AUG_ISISGLOBALAFISAFICONFAUG_RE_REDISTRIBUTION,
            IsisRedistributionListWriter(access),
            setOf(IIDs.NE_NE_PR_PR_IS_GL_AF_AF_AUG_ISISGLOBALAFISAFICONFAUG_RE_RE_CONFIG)
        )
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, access: UnderlayAccess) {
        rRegistry.add(IIDs.NE_NE_PR_PR_IS_GL_CONFIG, IsisGlobalConfigReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_IS_GL_AF_AF, IsisGlobalAfListReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_IS_GL_AF_AF_AUG_ISISGLOBALAFISAFICONFAUG_RE_REDISTRIBUTION,
            IsisRedistributionListReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_IS_GL_AF_AF_AUG_ISISGLOBALAFISAFICONFAUG_RE_RE_CONFIG,
            IsisRedistributionConfigReader(access))
    }

    override fun toString(): String = "translator for Cisco-IOS-XR-clns-isis-cfg@2018-11-23"
}