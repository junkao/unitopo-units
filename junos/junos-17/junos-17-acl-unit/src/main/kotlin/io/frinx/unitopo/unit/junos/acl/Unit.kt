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

package io.frinx.unitopo.unit.junos.acl

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.acl.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.acl.handler.AclInterfaceConfigReader
import io.frinx.unitopo.unit.junos.acl.handler.AclInterfaceReader
import io.frinx.unitopo.unit.junos.acl.handler.EgressAclSetConfigReader
import io.frinx.unitopo.unit.junos.acl.handler.EgressAclSetConfigWriter
import io.frinx.unitopo.unit.junos.acl.handler.EgressAclSetReader
import io.frinx.unitopo.unit.junos.acl.handler.IngressAclSetConfigReader
import io.frinx.unitopo.unit.junos.acl.handler.IngressAclSetConfigWriter
import io.frinx.unitopo.unit.junos.acl.handler.IngressAclSetReader
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.EgressAclSetsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.ingress.acl.top.IngressAclSetsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.top.AclBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.`$YangModuleInfoImpl` as AclYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo

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
            UnderlayInterfacesYangInfo.getInstance())

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
        wRegistry.add(GenericWriter(IIDs.ACL, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.AC_INTERFACES, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.AC_IN_INTERFACE, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.AC_IN_IN_INGRESSACLSETS, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.AC_IN_IN_IN_INGRESSACLSET, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.AC_IN_IN_IN_IN_CONFIG, IngressAclSetConfigWriter(underlayAccess)))
        wRegistry.add(GenericWriter(IIDs.AC_IN_IN_EGRESSACLSETS, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.AC_IN_IN_EG_EGRESSACLSET, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.AC_IN_IN_EG_EG_CONFIG, EgressAclSetConfigWriter(underlayAccess)))
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.ACL, AclBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.AC_INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.AC_IN_INTERFACE, AclInterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.AC_IN_IN_CONFIG, AclInterfaceConfigReader()))
        rRegistry.addStructuralReader(IIDs.AC_IN_IN_INGRESSACLSETS, IngressAclSetsBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.AC_IN_IN_IN_INGRESSACLSET, IngressAclSetReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.AC_IN_IN_IN_IN_CONFIG, IngressAclSetConfigReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.AC_IN_IN_EGRESSACLSETS, EgressAclSetsBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.AC_IN_IN_EG_EGRESSACLSET, EgressAclSetReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.AC_IN_IN_EG_EG_CONFIG, EgressAclSetConfigReader(underlayAccess)))
    }

    override fun toString(): String = "Junos 17.3 acl translate unit"
}