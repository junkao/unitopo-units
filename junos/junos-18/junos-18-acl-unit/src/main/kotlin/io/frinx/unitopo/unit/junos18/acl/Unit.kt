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

package io.frinx.unitopo.unit.junos18.acl

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.acl.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.acl.handler.AclInterfaceConfigReader
import io.frinx.unitopo.unit.junos18.acl.handler.AclInterfaceReader
import io.frinx.unitopo.unit.junos18.acl.handler.AclInterfaceWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.top.AclBuilder
import io.frinx.openconfig.openconfig.interfaces.IIDs as InterfaceIIDs
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.`$YangModuleInfoImpl` as AclYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.root.rev180101.`$YangModuleInfoImpl` as UnderlayConfRootYangModuleInfo

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas() = setOf(
            AclYangInfo.getInstance())

    override fun getUnderlayYangSchemas() = setOf(
            UnderlayConfRootYangModuleInfo.getInstance(),
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
        wRegistry.add(GenericWriter(IIDs.ACL, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.AC_INTERFACES, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.AC_IN_INTERFACE, AclInterfaceWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.AC_IN_IN_CONFIG, NoopWriter()), InterfaceIIDs.IN_IN_SU_SU_CONFIG)
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.ACL, AclBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.AC_INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.AC_IN_INTERFACE, AclInterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.AC_IN_IN_CONFIG, AclInterfaceConfigReader()))
    }

    override fun toString(): String = "Junos 18.2 acl translate unit"
}