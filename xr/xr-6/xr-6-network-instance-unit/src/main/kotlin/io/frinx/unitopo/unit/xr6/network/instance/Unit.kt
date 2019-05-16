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

package io.frinx.unitopo.unit.xr6.network.instance

import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.unitopo.ni.base.handler.vrf.ifc.VrfInterfaceConfigReader
import io.frinx.unitopo.ni.base.handler.vrf.protocol.ProtocolConfigReader
import io.frinx.unitopo.ni.base.handler.vrf.protocol.ProtocolStateReader
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.network.instance.handler.ConnectionPointsReader
import io.frinx.unitopo.unit.xr6.network.instance.handler.ConnectionPointsWriter
import io.frinx.unitopo.unit.xr6.network.instance.handler.NetworkInstanceConfigReader
import io.frinx.unitopo.unit.xr6.network.instance.handler.NetworkInstanceConfigWriter
import io.frinx.unitopo.unit.xr6.network.instance.handler.NetworkInstanceReader
import io.frinx.unitopo.unit.xr6.network.instance.handler.NetworkInstanceStateReader
import io.frinx.unitopo.unit.xr6.network.instance.handler.vrf.ifc.VrfInterfaceConfigWriter
import io.frinx.unitopo.unit.xr6.network.instance.handler.vrf.ifc.VrfInterfaceReader
import io.frinx.unitopo.unit.xr6.network.instance.handler.vrf.protocol.aggregate.LocalAggregateConfigReader
import io.frinx.unitopo.unit.xr6.network.instance.handler.vrf.protocol.aggregate.LocalAggregateConfigWriter
import io.frinx.unitopo.unit.xr6.network.instance.handler.vrf.protocol.aggregate.LocalAggregateReader
import io.frinx.unitopo.unit.xr6.network.instance.handler.vrf.protocol.ProtocolConfigWriter
import io.frinx.unitopo.unit.xr6.network.instance.handler.vrf.protocol.ProtocolReader
import io.frinx.unitopo.unit.xr6.network.instance.handler.vrf.table.TableConnectionConfigWriter
import io.frinx.unitopo.unit.xr6.network.instance.handler.vrf.table.TableConnectionReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.`$YangModuleInfoImpl`
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPoints
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.TableConnection
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayVRFYangInto

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {

    val CONN_PTS_ID = InstanceIdentifier.create(ConnectionPoints::class.java)

    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
            UnderlayInterfacesYangInfo.getInstance(),
            UnderlayVRFYangInto.getInstance()
    )

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(
        `$YangModuleInfoImpl`.getInstance()
    )

    override fun provideHandlers(
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        val checkRegistry = ChecksMap.getOpenconfigCheckRegistry()
        rRegistry.setCheckRegistry(checkRegistry)
        provideReaders(rRegistry, underlayAccess)
        wRegistry.setCheckRegistry(checkRegistry)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, underlay: UnderlayAccess) {
        // todo create proper writers once we support routing policies
        wRegistry.addNoop(IIDs.NE_NETWORKINSTANCE)
        wRegistry.addNoop(IIDs.NE_NE_INTERINSTANCEPOLICIES)
        wRegistry.addNoop(IIDs.NE_NE_IN_APPLYPOLICY)
        wRegistry.addNoop(IIDs.NE_NE_IN_AP_CONFIG)

        wRegistry.addNoop(IIDs.NE_NE_PR_PROTOCOL)
        wRegistry.addAfter(IIDs.NE_NE_PR_PR_CONFIG, ProtocolConfigWriter(underlay),
                IIDs.NE_NE_IN_INTERFACE)

        // Local aggregates
        wRegistry.addNoop(IIDs.NE_NE_PR_PR_LO_AGGREGATE)
        wRegistry.addAfter(IIDs.NE_NE_PR_PR_LO_AG_CONFIG, LocalAggregateConfigWriter(underlay),
                IIDs.NE_NE_CONFIG, IIDs.NE_NE_PR_PR_BG_GL_CONFIG, IIDs.NE_NE_PR_PR_OS_GL_CONFIG,
                IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_CONFIG)

        wRegistry.addNoop(IIDs.NE_NE_IN_INTERFACE)
        wRegistry.addAfter(IIDs.NE_NE_IN_IN_CONFIG, VrfInterfaceConfigWriter(underlay),
                IIDs.NE_NE_CONFIG)

        // Table connections for VRF
        wRegistry.addNoop(IIDs.NE_NE_TABLECONNECTIONS)
        wRegistry.addNoop(IIDs.NE_NE_TA_TABLECONNECTION)
        wRegistry.addAfter(IIDs.NE_NE_TA_TA_CONFIG, TableConnectionConfigWriter(underlay),
                /*add after protocol writers*/
                IIDs.NE_NE_PR_PR_CONFIG, IIDs.NE_NE_PR_PR_BG_GL_CONFIG, IIDs.NE_NE_PR_PR_OS_GL_CONFIG)

        wRegistry.addAfter(IIDs.NE_NE_CONFIG, NetworkInstanceConfigWriter(underlay),
                /*handle after ifc configuration*/ io.frinx.openconfig.openconfig.interfaces.IIDs.IN_IN_CONFIG,
                /*also after subinterface*/
                    io.frinx.openconfig.openconfig.vlan.IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VL_CONFIG)

        wRegistry.subtreeAddAfter(IIDs.NE_NE_CONNECTIONPOINTS, ConnectionPointsWriter(underlay),
            setOf(
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CONNECTIONPOINT, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_CONFIG, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_ENDPOINTS, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_ENDPOINT, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_CONFIG, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_LOCAL, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_LO_CONFIG, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_REMOTE, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_RE_CONFIG, CONN_PTS_ID)
            ),
                /*handle after network instance configuration*/ IIDs.NE_NE_CONFIG)
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlay: UnderlayAccess) {
        rRegistry.add(IIDs.NE_NETWORKINSTANCE, NetworkInstanceReader(underlay))
        rRegistry.add(IIDs.NE_NE_CONFIG, NetworkInstanceConfigReader(underlay))
        rRegistry.add(IIDs.NE_NE_STATE, NetworkInstanceStateReader(underlay))

        rRegistry.add(IIDs.NE_NE_IN_INTERFACE, VrfInterfaceReader(underlay))
        rRegistry.add(IIDs.NE_NE_IN_IN_CONFIG, VrfInterfaceConfigReader())

        rRegistry.add(IIDs.NE_NE_PR_PR_CONFIG, ProtocolConfigReader())
        rRegistry.add(IIDs.NE_NE_PR_PR_STATE, ProtocolStateReader())
        rRegistry.add(IIDs.NE_NE_PR_PROTOCOL, ProtocolReader(underlay))

        // Local aggregates
        rRegistry.add(IIDs.NE_NE_PR_PR_LO_AGGREGATE, LocalAggregateReader(underlay))
        rRegistry.add(IIDs.NE_NE_PR_PR_LO_AG_CONFIG, LocalAggregateConfigReader(underlay))

        // Table connections for VRF
        rRegistry.subtreeAdd(IIDs.NE_NE_TA_TABLECONNECTION, TableConnectionReader(underlay),
            setOf(RWUtils.cutIdFromStart<TableConnection>(IIDs.NE_NE_TA_TA_CONFIG,
                InstanceIdentifier.create(TableConnection::class.java))))

        // Connection points for L2P2p
        rRegistry.subtreeAdd(IIDs.NE_NE_CONNECTIONPOINTS, ConnectionPointsReader(underlay),
            setOf(
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CONNECTIONPOINT, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_CONFIG, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_STATE, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_ENDPOINTS, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_ENDPOINT, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_CONFIG, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_STATE, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_LOCAL, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_LO_CONFIG, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_LO_STATE, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_REMOTE, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_RE_CONFIG, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_RE_STATE, CONN_PTS_ID))
            )
    }

    override fun toString(): String = "XR 6 (2015-07-30) network-instance translate unit"

    override fun useAutoCommit() = true
}