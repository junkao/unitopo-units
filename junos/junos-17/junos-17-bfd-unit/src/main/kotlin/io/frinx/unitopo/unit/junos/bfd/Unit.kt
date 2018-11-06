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

package io.frinx.unitopo.unit.junos.bfd

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.bfd.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.bfd.handler.BfdConfigReader
import io.frinx.unitopo.unit.junos.bfd.handler.ConfigWriter
import io.frinx.unitopo.unit.junos.bfd.handler.InterfaceReader
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.rev171117.bfd.top.bfd.interfaces._interface.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.rev171117.`$YangModuleInfoImpl` as BfdBaseYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.ext.rev180211.`$YangModuleInfoImpl` as BfdExtYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.ext.rev180211.IfBfdExtAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.rev171117.bfd.top.BfdBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.rev171117.bfd.top.bfd.InterfacesBuilder

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    @Suppress("unused")
    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    @Suppress("unused")
    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas() = setOf(
        BfdBaseYangInfo.getInstance(),
        BfdExtYangInfo.getInstance()
    )

    override fun getUnderlayYangSchemas() = setOf(
        UnderlayInterfacesYangInfo.getInstance()
    )

    override fun getRpcs(underlayAccess: UnderlayAccess) = emptySet<RpcService<*, *>>()

    override fun provideHandlers(
        rRegistry: ModifiableReaderRegistryBuilder,
        wRegistry: ModifiableWriterRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.add(GenericWriter(IIDs.BFD, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.BF_INTERFACES, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.BF_IN_INTERFACE, NoopListWriter()))
        wRegistry.subtreeAdd(setOf(InstanceIdentifier.create(Config::class.java).augmentation(IfBfdExtAug::class.java)),
            GenericWriter(IIDs.BF_IN_IN_CONFIG, ConfigWriter(underlayAccess)))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.BFD, BfdBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.BF_INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.BF_IN_INTERFACE, InterfaceReader(underlayAccess)))
        rRegistry.subtreeAdd(setOf(InstanceIdentifier.create(Config::class.java).augmentation(IfBfdExtAug::class.java)),
            GenericConfigReader(IIDs.BF_IN_IN_CONFIG, BfdConfigReader(underlayAccess)))
    }

    override fun toString(): String = "Junos 17.3 BFD translation unit"
}