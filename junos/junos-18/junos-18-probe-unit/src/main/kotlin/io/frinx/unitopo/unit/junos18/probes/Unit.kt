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

package io.frinx.unitopo.unit.junos18.probes

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.read.GenericListReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.probes.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.probes.handler.ProbeConfigReader
import io.frinx.unitopo.unit.junos18.probes.handler.ProbeConfigWriter
import io.frinx.unitopo.unit.junos18.probes.handler.ProbeReader
import io.frinx.unitopo.unit.junos18.probes.handler.test.ProbeTargetConfigReader
import io.frinx.unitopo.unit.junos18.probes.handler.test.ProbeTargetConfigWriter
import io.frinx.unitopo.unit.junos18.probes.handler.test.ProbeTestConfigReader
import io.frinx.unitopo.unit.junos18.probes.handler.test.ProbeTestConfigWriter
import io.frinx.unitopo.unit.junos18.probes.handler.test.ProbeTestReader
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.openconfig.probes.top.ProbesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.probe.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.probe.TestsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.test.target.TargetBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.juniper.rev181203.`$YangModuleInfoImpl` as ProbeJunosExtYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.`$YangModuleInfoImpl` as ProbeYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.test.Config as TestConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.types.rev170905.`$YangModuleInfoImpl` as ProbeTypesYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.common.types.rev180101.`$YangModuleInfoImpl` as UnderlayJunosCommonTypesYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.root.rev180101.`$YangModuleInfoImpl` as UnderlayConfRootYangModuleInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.`$YangModuleInfoImpl` as UnderlayServicesYangInfo
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas() = setOf(
        ProbeYangInfo.getInstance(),
        ProbeJunosExtYangInfo.getInstance(),
        ProbeTypesYangInfo.getInstance()
    )

    override fun getUnderlayYangSchemas() = setOf(
        UnderlayConfRootYangModuleInfo.getInstance(),
        UnderlayServicesYangInfo.getInstance(),
        UnderlayJunosCommonTypesYangInfo.getInstance()
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
        wRegistry.add(GenericListWriter(IIDs.PR_PROBE, NoopListWriter()))
        wRegistry.subtreeAdd(PR_PR_CONFIG_SUBTREE,
            GenericWriter(IIDs.PR_PR_CONFIG, ProbeConfigWriter(underlayAccess)))

        wRegistry.add(GenericWriter(IIDs.PR_PR_TESTS, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.PR_PR_TE_TEST, NoopListWriter()))
        wRegistry.subtreeAdd(PR_PR_TE_TE_CONFIG_SUBTREE,
            GenericWriter(IIDs.PR_PR_TE_TE_CONFIG, ProbeTestConfigWriter(underlayAccess)))

        wRegistry.add(GenericWriter(IIDs.PR_PR_TE_TE_TARGET, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.PR_PR_TE_TE_TA_CONFIG, ProbeTargetConfigWriter(underlayAccess)))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.PROBES, ProbesBuilder::class.java)
        rRegistry.add(GenericListReader(IIDs.PR_PROBE, ProbeReader(underlayAccess)))
        rRegistry.subtreeAdd(PR_PR_CONFIG_SUBTREE,
            GenericConfigReader(IIDs.PR_PR_CONFIG, ProbeConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IIDs.PR_PR_TESTS, TestsBuilder::class.java)
        rRegistry.add(GenericListReader(IIDs.PR_PR_TE_TEST, ProbeTestReader(underlayAccess)))
        rRegistry.subtreeAdd(PR_PR_TE_TE_CONFIG_SUBTREE,
            GenericConfigReader(IIDs.PR_PR_TE_TE_CONFIG, ProbeTestConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IIDs.PR_PR_TE_TE_TARGET, TargetBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.PR_PR_TE_TE_TA_CONFIG, ProbeTargetConfigReader(underlayAccess)))
    }

    override fun toString(): String = "Junos 18.2 probe translate unit"

    companion object {
        private val PR_PR_CONFIG_SUBTREE_ROOT = IID.create(Config::class.java)
        private val PR_PR_CONFIG_SUBTREE = setOf(
            RWUtils.cutIdFromStart(IIDs.PR_PR_CO_AUG_CONFIG3, PR_PR_CONFIG_SUBTREE_ROOT)
        )

        private val PR_PR_TE_TE_CONFIG_SUBTREE_ROOT = IID.create(TestConfig::class.java)
        private val PR_PR_TE_TE_CONFIG_SUBTREE = setOf(
            RWUtils.cutIdFromStart(IIDs.PR_PR_TE_TE_CO_AUG_CONFIG4, PR_PR_TE_TE_CONFIG_SUBTREE_ROOT)
        )
    }
}