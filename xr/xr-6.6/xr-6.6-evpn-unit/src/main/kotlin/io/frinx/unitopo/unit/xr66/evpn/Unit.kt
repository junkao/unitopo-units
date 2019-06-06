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

package io.frinx.unitopo.unit.xr66.evpn

import com.google.common.collect.Sets
import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.evpn.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.xr66.evpn.handler.group.EvpnGroupListWriter
import io.frinx.unitopo.unit.xr66.evpn.handler.group.coreifc.EvpnGroupCoreInterfaceConfigWriter
import io.frinx.unitopo.unit.xr66.evpn.handler.ifc.EvpnInterfaceListWriter
import io.frinx.unitopo.unit.xr66.evpn.handler.ifc.core.isolation.group.EvpnCoreIsolationGroupConfigReader
import io.frinx.unitopo.unit.xr66.evpn.handler.ifc.core.isolation.group.EvpnCoreIsolationGroupConfigWriter
import io.frinx.unitopo.unit.xr66.evpn.handler.ifc.es.EvpnEthernetSegmentConfigReader
import io.frinx.unitopo.unit.xr66.evpn.handler.ifc.es.EvpnEthernetSegmentConfigWriter
import io.frinx.unitopo.unit.xr66.evpn.handler.group.coreifc.EvpnGroupCoreInterfaceListReader
import io.frinx.unitopo.unit.xr66.evpn.handler.group.EvpnGroupListReader
import io.frinx.unitopo.unit.xr66.evpn.handler.ifc.EvpnInterfaceListReader
import io.frinx.unitopo.unit.xr66.init.Unit
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.GroupsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.Group
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.CoreInterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.CoreIsolationGroupBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.EthernetSegmentBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.top.Evpn
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.`$YangModuleInfoImpl` as UnderlayEvpnsYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.`$YangModuleInfoImpl` as EvpnYangInfo
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.core.interfaces.Interface as CoreInterface

class Unit(private val registry: TranslationUnitCollector) : Unit() {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas() = setOf(
        EvpnYangInfo.getInstance()
    )

    override fun getUnderlayYangSchemas() = UNDERLAY_SCHEMAS

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
        wRegistry.subtreeAdd(Sets.newHashSet<IID<*>>(RWUtils.cutIdFromStart(IIDs.EV_CONFIG,
            IID.create(Evpn::class.java))),
            GenericWriter(IIDs.EVPN, EvpnWriter(underlayAccess)))
        // groups
        wRegistry.subtreeAdd(Sets.newHashSet<IID<*>>(RWUtils.cutIdFromStart(IIDs.EV_GR_GR_CONFIG,
            IID.create(Group::class.java))),
            GenericListWriter(IIDs.EV_GR_GROUP, EvpnGroupListWriter(underlayAccess)))
        wRegistry.add(GenericListWriter(IIDs.EV_GR_GR_CO_INTERFACE, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.EV_GR_GR_CO_IN_CONFIG,
            EvpnGroupCoreInterfaceConfigWriter(underlayAccess)))

        // interfaces
        wRegistry.subtreeAdd(Sets.newHashSet<IID<*>>(RWUtils.cutIdFromStart(IIDs.EV_IN_IN_CONFIG,
            IID.create(Interface::class.java))),
            GenericListWriter(IIDs.EV_IN_INTERFACE, EvpnInterfaceListWriter(underlayAccess)))
        wRegistry.add(GenericWriter(IIDs.EV_IN_IN_ET_CONFIG, EvpnEthernetSegmentConfigWriter(underlayAccess)))
        wRegistry.add(GenericWriter(IIDs.EV_IN_IN_CO_CONFIG, EvpnCoreIsolationGroupConfigWriter(underlayAccess)))
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.add(GenericConfigReader(IIDs.EVPN, EvpnReader(underlayAccess)))
        // groups
        rRegistry.addStructuralReader(IIDs.EV_GROUPS, GroupsBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.EV_GR_GROUP, EvpnGroupListReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.EV_GR_GR_COREINTERFACES, CoreInterfacesBuilder::class.java)
        rRegistry.subtreeAdd(
            setOf(
                RWUtils.cutIdFromStart(IIDs.EV_GR_GR_CO_IN_CONFIG,
                    IID.create(CoreInterface::class.java))),
            GenericConfigListReader(IIDs.EV_GR_GR_CO_INTERFACE,
                EvpnGroupCoreInterfaceListReader(underlayAccess)))
        // interfaces
        rRegistry.addStructuralReader(IIDs.EV_INTERFACES, InterfacesBuilder::class.java)
        rRegistry.subtreeAdd(
            setOf(RWUtils.cutIdFromStart(IIDs.EV_IN_IN_CONFIG,
                IID.create(Interface::class.java))),
            GenericConfigListReader(IIDs.EV_IN_INTERFACE, EvpnInterfaceListReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.EV_IN_IN_ETHERNETSEGMENT, EthernetSegmentBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.EV_IN_IN_ET_CONFIG,
            EvpnEthernetSegmentConfigReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.EV_IN_IN_COREISOLATIONGROUP, CoreIsolationGroupBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.EV_IN_IN_CO_CONFIG,
            EvpnCoreIsolationGroupConfigReader(underlayAccess)))
    }

    override fun toString(): String = "Cisco-IOS-XR 7.0.1 (2018-06-15) l2vpn-cfg:evpn translate unit"

    companion object {
        val UNDERLAY_SCHEMAS = setOf(
            UnderlayEvpnsYangInfo.getInstance()
        )
    }
}