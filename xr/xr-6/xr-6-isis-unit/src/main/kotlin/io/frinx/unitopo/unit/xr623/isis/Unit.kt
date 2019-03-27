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
package io.frinx.unitopo.unit.xr623.isis

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr623.isis.handler.interfaces.IsisInterfaceAuthConfigReader
import io.frinx.unitopo.unit.xr623.isis.handler.interfaces.IsisInterfaceAuthConfigWriter
import io.frinx.unitopo.unit.xr623.isis.handler.interfaces.IsisInterfaceConfigReader
import io.frinx.unitopo.unit.xr623.isis.handler.interfaces.IsisInterfaceConfigWriter
import io.frinx.unitopo.unit.xr623.isis.handler.interfaces.IsisInterfaceReader
import io.frinx.unitopo.unit.xr623.isis.handler.interfaces.IsisInterfaceTimersConfigReader
import io.frinx.unitopo.unit.xr623.isis.handler.interfaces.IsisInterfaceTimersConfigWriter
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.`$YangModuleInfoImpl` as UnderlayIsisConfigYangModule
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.`$YangModuleInfoImpl` as UnderlayIsisTypesYangModule
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
        access: UnderlayAccess
    ) {
        provideReaders(rRegistry, access)
        provideWriters(wRegistry, access)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, access: UnderlayAccess) {
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_ISIS)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_IS_INTERFACES)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_IS_IN_INTERFACE)
        wRegistry.subtreeAdd(setOf(RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_IS_IN_IN_CO_AUG_ISISIFCONFAUG,
                IIDs.NE_NE_PR_PR_IS_IN_IN_CONFIG)),
                GenericWriter(IIDs.NE_NE_PR_PR_IS_IN_IN_CONFIG, IsisInterfaceConfigWriter(access)))

        wRegistry.addNoop(IIDs.NE_NE_PR_PR_IS_IN_IN_AUTHENTICATION)
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_IS_IN_IN_AU_KEY)
        wRegistry.add(IIDs.NE_NE_PR_PR_IS_IN_IN_AU_KE_CONFIG, IsisInterfaceAuthConfigWriter(access))

        wRegistry.addNoop(IIDs.NE_NE_PR_PR_IS_IN_IN_TIMERS)
        wRegistry.subtreeAdd(IIDs.NE_NE_PR_PR_IS_IN_IN_TI_CONFIG,
            IsisInterfaceTimersConfigWriter(access),
            setOf(RWUtils.cutIdFromStart(
                IIDs.NE_NE_PR_PR_IS_IN_IN_TI_CO_AUG_ISISIFTIMERSCONFAUG, IIDs.NE_NE_PR_PR_IS_IN_IN_TI_CONFIG)))
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, access: UnderlayAccess) {
        rRegistry.add(IIDs.NE_NE_PR_PR_IS_IN_INTERFACE, IsisInterfaceReader(access))
        rRegistry.subtreeAdd(IIDs.NE_NE_PR_PR_IS_IN_IN_CONFIG, IsisInterfaceConfigReader(access),
            setOf(RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_IS_IN_IN_CO_AUG_ISISIFCONFAUG,
                IIDs.NE_NE_PR_PR_IS_IN_IN_CONFIG)))

        rRegistry.add(IIDs.NE_NE_PR_PR_IS_IN_IN_AU_KE_CONFIG, IsisInterfaceAuthConfigReader(access))

        rRegistry.subtreeAdd(IIDs.NE_NE_PR_PR_IS_IN_IN_TI_CONFIG, IsisInterfaceTimersConfigReader(access),
            setOf(RWUtils.cutIdFromStart(IIDs.NE_NE_PR_PR_IS_IN_IN_TI_CO_AUG_ISISIFTIMERSCONFAUG,
                IIDs.NE_NE_PR_PR_IS_IN_IN_TI_CONFIG)))
    }

    override fun toString(): String = "XR 6 (2019-03-15) ISIS translate unit"
}