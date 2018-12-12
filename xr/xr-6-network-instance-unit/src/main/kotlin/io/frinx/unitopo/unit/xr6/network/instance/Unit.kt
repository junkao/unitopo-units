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

import com.google.common.collect.Sets
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.handlers.network.instance.NetworkInstanceUnit
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import io.frinx.unitopo.unit.xr6.network.instance.vrf.ifc.VrfInterfaceConfigWriter
import io.frinx.unitopo.unit.xr6.network.instance.vrf.ifc.VrfInterfaceReader
import io.frinx.unitopo.unit.xr6.network.instance.vrf.protocol.LocalAggregateConfigReader
import io.frinx.unitopo.unit.xr6.network.instance.vrf.protocol.LocalAggregateConfigWriter
import io.frinx.unitopo.unit.xr6.network.instance.vrf.protocol.LocalAggregateReader
import io.frinx.unitopo.unit.xr6.network.instance.vrf.protocol.ProtocolConfigWriter
import io.frinx.unitopo.unit.xr6.network.instance.vrf.protocol.ProtocolReader
import io.frinx.unitopo.unit.xr6.network.instance.vrf.table.TableConnectionConfigWriter
import io.frinx.unitopo.unit.xr6.network.instance.vrf.table.TableConnectionReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.LocalAggregatesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPoints
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.TableConnectionsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.TableConnection
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayVRFYangInto

class Unit(private val registry: TranslationUnitCollector) : NetworkInstanceUnit() {

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

    override fun provideSpecificWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        // todo create proper writers once we support routing policies
        wRegistry.add(GenericWriter(IIDs.NE_NE_INTERINSTANCEPOLICIES, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_IN_APPLYPOLICY, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_IN_AP_CONFIG, NoopWriter()))

        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_PR_PR_CONFIG, ProtocolConfigWriter(underlayAccess)),
                IIDs.NE_NE_IN_INTERFACE)

        // Local aggregates
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PR_LO_AGGREGATE, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_PR_PR_LO_AG_CONFIG, LocalAggregateConfigWriter(underlayAccess)),
                Sets.newHashSet(IIDs.NE_NE_CONFIG, IIDs.NE_NE_PR_PR_BG_GL_CONFIG, IIDs.NE_NE_PR_PR_OS_GL_CONFIG,
                        IIDs.NE_NE_PR_PR_BG_GL_AF_AF_CONFIG, IIDs.NE_NE_PR_PR_BG_NE_NE_AF_AF_CONFIG))

        wRegistry.add(GenericWriter(IIDs.NE_NE_IN_INTERFACE, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_IN_IN_CONFIG, VrfInterfaceConfigWriter(underlayAccess)),
                IIDs.NE_NE_CONFIG)

        // Table connections for VRF
        wRegistry.add(GenericWriter(IIDs.NE_NE_TABLECONNECTIONS, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_TA_TABLECONNECTION, NoopListWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_TA_TA_CONFIG, TableConnectionConfigWriter(underlayAccess)),
                /*add after protocol writers*/
                Sets.newHashSet(IIDs.NE_NE_PR_PR_CONFIG, IIDs.NE_NE_PR_PR_BG_GL_CONFIG, IIDs.NE_NE_PR_PR_OS_GL_CONFIG))

        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_CONFIG, NetworkInstanceConfigWriter(underlayAccess)),
                setOf(
                /*handle after ifc configuration*/ io.frinx.openconfig.openconfig.interfaces.IIDs.IN_IN_CONFIG,
                /*also after subinterface*/
                    io.frinx.openconfig.openconfig.vlan.IIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VL_CONFIG))

        wRegistry.subtreeAddAfter(Sets.newHashSet<InstanceIdentifier<*>>(
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CONNECTIONPOINT, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_CONFIG, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_ENDPOINTS, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_ENDPOINT, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_CONFIG, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_LOCAL, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_LO_CONFIG, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_REMOTE, CONN_PTS_ID),
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_RE_CONFIG, CONN_PTS_ID)
        ), GenericWriter(IIDs.NE_NE_CONNECTIONPOINTS, ConnectionPointsWriter(underlayAccess)),
                /*handle after network instance configuration*/ IIDs.NE_NE_CONFIG)
    }

    override fun provideSpecificReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {

        rRegistry.add(GenericConfigListReader(IIDs.NE_NETWORKINSTANCE, NetworkInstanceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader<Config, ConfigBuilder>(IIDs.NE_NE_CONFIG,
            NetworkInstanceConfigReader(underlayAccess)))

        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_IN_INTERFACE, VrfInterfaceReader(underlayAccess)))

        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PROTOCOL, ProtocolReader(underlayAccess)))

        // Local aggregates
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_LOCALAGGREGATES, LocalAggregatesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PR_PR_LO_AGGREGATE, LocalAggregateReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_LO_AG_CONFIG, LocalAggregateConfigReader(underlayAccess)))

        // Table connections for VRF
        rRegistry.addStructuralReader(IIDs.NE_NE_TABLECONNECTIONS, TableConnectionsBuilder::class.java)
        rRegistry.subtreeAdd(setOf(RWUtils.cutIdFromStart<TableConnection>(IIDs.NE_NE_TA_TA_CONFIG,
            InstanceIdentifier.create(TableConnection::class.java))),
                GenericConfigListReader(IIDs.NE_NE_TA_TABLECONNECTION, TableConnectionReader(underlayAccess)))

        // Connection points for L2P2p
        rRegistry.subtreeAdd(Sets.newHashSet<InstanceIdentifier<*>>(
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
                RWUtils.cutIdFromStart(IIDs.NE_NE_CO_CO_EN_EN_RE_STATE, CONN_PTS_ID)),
                GenericConfigReader(IIDs.NE_NE_CONNECTIONPOINTS, ConnectionPointsReader(underlayAccess)))
    }

    override fun toString(): String = "XR 6 (2015-07-30) network-instance translate unit"
}