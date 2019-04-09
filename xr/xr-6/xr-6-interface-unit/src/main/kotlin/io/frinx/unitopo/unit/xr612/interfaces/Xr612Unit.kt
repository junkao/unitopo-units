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

package io.frinx.unitopo.unit.xr612.interfaces

import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.CommonUnit
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev160512.`$YangModuleInfoImpl` as UnderlayBundleMgrYangInfo

class Xr612Unit(private val registry: TranslationUnitCollector) : CommonUnit(registry) {
    override fun provideSpecificWriters(
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        // NOOP
    }

    override fun provideSpecificReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess) {
        // NOOP
    }

    override fun getSpecificYangSchemas() = emptySet<YangModuleInfo>()

    override fun getSpecificUnderlayYangSchemas() = setOf(UnderlayBundleMgrYangInfo.getInstance())

    override fun toString(): String = "XR 6 (2015-07-30) interface translate unit"
}