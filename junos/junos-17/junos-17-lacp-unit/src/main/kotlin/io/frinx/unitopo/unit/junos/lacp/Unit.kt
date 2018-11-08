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

package io.frinx.unitopo.unit.junos.lacp

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.lacp.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.lacp.handler.BundleConfigReader
import io.frinx.unitopo.unit.junos.lacp.handler.BundleConfigWriter
import io.frinx.unitopo.unit.junos.lacp.handler.BundleReader
import io.frinx.unitopo.unit.junos.lacp.handler.MemberConfigReader
import io.frinx.unitopo.unit.junos.lacp.handler.MemberConfigWriter
import io.frinx.unitopo.unit.junos.lacp.handler.MemberReader
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.aggregation.lacp.members.top.MembersBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.aggregation.lacp.top.LacpBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.lacp.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.`$YangModuleInfoImpl` as LacpBaseYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.lag.member.rev171109.`$YangModuleInfoImpl` as LacpExtYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo

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
        LacpBaseYangInfo.getInstance(),
        LacpExtYangInfo.getInstance()
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
        // LACP root
        wRegistry.add(GenericWriter(IIDs.LACP, NoopWriter()))

        // bundle interface
        wRegistry.add(GenericWriter(IIDs.LA_INTERFACES, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.LA_IN_INTERFACE, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.LA_IN_IN_CONFIG, BundleConfigWriter(underlayAccess)))

        // member's interface
        wRegistry.add(GenericWriter(IIDs.LA_IN_IN_MEMBERS, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.LA_IN_IN_ME_MEMBER, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.LA_IN_IN_ME_ME_CONFIG, MemberConfigWriter(underlayAccess)))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        // LACP root
        rRegistry.addStructuralReader(IIDs.LACP, LacpBuilder::class.java)

        // bundle interface
        rRegistry.addStructuralReader(IIDs.LA_INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.LA_IN_INTERFACE, BundleReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.LA_IN_IN_CONFIG, BundleConfigReader(underlayAccess)))

        // member's interface
        rRegistry.addStructuralReader(IIDs.LA_IN_IN_MEMBERS, MembersBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.LA_IN_IN_ME_MEMBER, MemberReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.LA_IN_IN_ME_ME_CONFIG, MemberConfigReader()))
    }

    override fun toString(): String = "Junos 17.3 LACP translation unit"
}