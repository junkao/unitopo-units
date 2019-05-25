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

package io.frinx.unitopo.unit.xr6.network.instance.handler.l2p2p.cp

import com.google.common.collect.Lists
import io.fd.honeycomb.translate.spi.builder.BasicCheck
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.SubinterfaceConfigWriter
import io.frinx.unitopo.unit.xr6.network.instance.handler.l2p2p.L2P2PReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceModeEnum
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109._interface.configurations._interface.configuration.EthernetServiceBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109._interface.configurations._interface.configuration.VlanSubConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109._interface.configurations._interface.configuration.ethernet.service.EncapsulationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109._interface.configurations._interface.configuration.ethernet.service.RewriteBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109._interface.configurations._interface.configuration.vlan.sub.configuration.VlanIdentifierBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.Match
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.Rewrite
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.Vlan
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.VlanTag
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.VlanTagOrAny
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.PseudowireIdRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109._interface.configurations._interface.configuration.L2Transport
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109._interface.configurations._interface.configuration.L2TransportBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnectKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuits
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.attachment.circuits.AttachmentCircuit
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.attachment.circuits.AttachmentCircuitBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.attachment.circuits.AttachmentCircuitKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.Pseudowire
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.PseudowireBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.PseudowireKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.pseudowire.NeighborBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.Subinterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPoints
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.ConnectionPoint
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.Endpoint
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.endpoint.Local
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.connection.points.connection.point.endpoints.endpoint.Remote
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.LOCAL
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.REMOTE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.Subinterface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.vlan.Config
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109.InterfaceConfiguration1 as VlanSubConfigurationAugmentation
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109.InterfaceConfiguration1Builder as VlanSubConfigurationAugmentationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109.InterfaceConfiguration2 as UnderlayIfcEthernetServiceAug
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109.InterfaceConfiguration2Builder as UnderlayIfcEthernetServiceAugBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.InterfaceConfiguration3 as UnderlayIfcL2TransportAug
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.InterfaceConfiguration3Builder as UnderlayIfcL2TransportAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface1 as IpSubInterfaceAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.Vlan as OpenConfigVlan

class L2P2PConnectionPointsWriter(private val underlayAccess: UnderlayAccess)
    : CompositeWriter.Child<ConnectionPoints> {

    override fun writeCurrentAttributesWResult(
        id: InstanceIdentifier<ConnectionPoints>,
        dataAfter: ConnectionPoints,
        writeContext: WriteContext
    ): Boolean {
        if (!BasicCheck.checkData(ChecksMap.DataCheck.NetworkInstanceConfig.IID_TRANSFORMATION,
                ChecksMap.DataCheck.NetworkInstanceConfig.TYPE_L2P2P).canProcess(id, writeContext, false)) {
            return false
        }

        val connectionPointList = dataAfter.connectionPoint
                ?: throw IllegalArgumentException("No connection points specified")

        require(connectionPointList.size == 2) {
            "L2P2P network only supports 2 endpoints, were ${connectionPointList.size}"
        }

        val (endpoint1, entpoint2) = getEndPoints(dataAfter)

        configureEndpoint(writeContext, endpoint1, id)
        configureEndpoint(writeContext, entpoint2, id)
        return true
    }

    private fun getEndPoints(config: ConnectionPoints): Pair<Endpoint, Endpoint> {
        val connectionPointList = config.connectionPoint
                ?: throw IllegalArgumentException("No connection points specified")

        require(connectionPointList.size == 2) {
            "L2P2P network only supports 2 endpoints, were ${connectionPointList.size}"
        }

        val endpoint1 = getEndpoint(connectionPointList, L2P2PConnectionPointsReader.CONNECTION_POINT_1)
        val endpoint2 = getEndpoint(connectionPointList, L2P2PConnectionPointsReader.CONNECTION_POINT_2)

        return Pair(endpoint1, endpoint2)
    }

    private fun getEndpoint(connectionPointList: List<ConnectionPoint>, connectionPointId: String): Endpoint {
        val connectionPoint = connectionPointList.find { it.connectionPointId == connectionPointId }
                ?: throw IllegalArgumentException("No connection point $connectionPointId")

        require(connectionPoint.endpoints.endpoint?.size == 1)

        val endPoint = connectionPoint.endpoints.endpoint?.firstOrNull()
                ?: throw IllegalArgumentException("No endpoint in connection point $connectionPointId")

        require(endPoint.endpointId == L2P2PConnectionPointsReader.ENDPOINT_ID)

        return endPoint
    }

    private fun configureEndpoint(
        writeContext: WriteContext,
        endpoint: Endpoint,
        id: InstanceIdentifier<ConnectionPoints>
    ) {
        if (endpoint.config?.type === LOCAL::class.java) {
            configureLocalEndpoint(writeContext, endpoint.local, id)
        } else {
            configureRemote(endpoint.remote, id)
        }
    }

    private fun configureRemote(remote: Remote, id: InstanceIdentifier<ConnectionPoints>) {

        val l2p2InstanceName = id.firstKeyOf(NetworkInstance::class.java).name

        val underlayP2PXconnectId = L2P2PReader.UNDERLAY_P2PXCONNECT_ID.child(P2pXconnect::class.java,
                P2pXconnectKey(CiscoIosXrString(l2p2InstanceName)))

        val circuitId = remote.config?.virtualCircuitIdentifier
        val remoteSystem = remote.config?.remoteSystem?.ipv4Address

        val underlayPseudowireId = underlayP2PXconnectId.child(Pseudowires::class.java)
                .child(Pseudowire::class.java, PseudowireKey(PseudowireIdRange(circuitId)))

        val pseudowire = PseudowireBuilder()
                .setNeighbor(Lists.newArrayList(NeighborBuilder().setNeighbor(Ipv4AddressNoZone(remoteSystem)).build()))
                .setPseudowireId(PseudowireIdRange(circuitId))
                .build()

        underlayAccess.merge(underlayPseudowireId, pseudowire)
    }

    private fun configureLocalEndpoint(
        writeContext: WriteContext,
        local: Local,
        id: InstanceIdentifier<ConnectionPoints>
    ) {
        val ifcName: String
        if (local.config?.subinterface != null) {
            ifcName = local.config?.`interface`!! + "." + local.config?.subinterface!!
            configureL2Subifc(writeContext, local.config?.`interface`!!, local.config?.subinterface!! as Long)
        } else {
            ifcName = local.config?.`interface`!!
            configureUnderlayL2Interface(writeContext, local.config?.`interface`!!)
        }

        configureUnderlayAttachementCircuit(ifcName, id)
    }

    private fun configureUnderlayAttachementCircuit(ifcName: String, id: InstanceIdentifier<ConnectionPoints>) {

        val l2p2InstanceName = id.firstKeyOf(NetworkInstance::class.java).name

        val underlayP2PXconnectId = L2P2PReader.UNDERLAY_P2PXCONNECT_ID.child(P2pXconnect::class.java,
                P2pXconnectKey(CiscoIosXrString(l2p2InstanceName)))

        val attachementCircuit = AttachmentCircuitBuilder().setEnable(true)
                .setName(InterfaceName(ifcName))
                .build()

        val attachementCircuitId = underlayP2PXconnectId.child(AttachmentCircuits::class.java)
                .child(AttachmentCircuit::class.java, AttachmentCircuitKey(InterfaceName(ifcName)))

        underlayAccess.merge(attachementCircuitId, attachementCircuit)
    }

    private fun getUnderlayIfcId(underlayIfcName: String): InstanceIdentifier<InterfaceConfiguration> {
        val interfaceActive = InterfaceActive("act")

        return InterfaceReader.IFC_CFGS
                .child(InterfaceConfiguration::class.java,
                        InterfaceConfigurationKey(interfaceActive, InterfaceName(underlayIfcName)))
    }

    private fun configureUnderlayL2Interface(writeContext: WriteContext, underlayIfcName: String) {
        checkIfcExists(writeContext, underlayIfcName)
        checkIfcNoIp(writeContext, underlayIfcName)
        // FIXME add a check whether subinterface is present. XR does not allow subinterfaces to exist when configuring
        // parent interface with L2P2P
        val underlayIfcId = getUnderlayIfcId(underlayIfcName)
        val underlayL2TransportIfcAugId = underlayIfcId.augmentation(UnderlayIfcL2TransportAug::class.java)

        val underlayL2TransportData = UnderlayIfcL2TransportAugBuilder()
                .setL2Transport(L2TransportBuilder().setEnabled(true).build())
                .build()

        underlayAccess.merge(underlayL2TransportIfcAugId, underlayL2TransportData)
    }

    private fun configureL2Subifc(writeContext: WriteContext, underlayIfcName: String, vlanId: Long) {

        val subIfcId = getSubifcId(underlayIfcName, vlanId)
        val subinterfaceData = checkSubIfcExists(writeContext, subIfcId)
        checkVlanConfigured(writeContext, underlayIfcName, vlanId)

        val underlayIfcId = getUnderlayIfcId(underlayIfcName + "." + vlanId)

        val underlayEthernetService = EthernetServiceBuilder()
                .setRewrite(RewriteBuilder().setRewriteType(Rewrite.Pop1).build())
                .setEncapsulation(EncapsulationBuilder()
                        .setOuterTagType(Match.MatchDot1q)
                        .setOuterRange1Low(VlanTagOrAny(vlanId)).build())
                .build()

        val underlayIfcEthernetServiceAug = UnderlayIfcEthernetServiceAugBuilder()
                .setEthernetService(underlayEthernetService)
                .build()

        // Reconfigure existing subinterface by adding l2transport flag to it, to make it available for l2p2p

        // Underlay interface configuration representing subinterface is needed here, so read it from underlay
        // or if not present (we are configuring it right now) invoke SubinterfaceWriter to render the data
        val subinterfaceFromUnderlay = underlayAccess.read(underlayIfcId).get()
        val subinterfaceToConfigure = SubinterfaceConfigWriter(underlayAccess).getData(subinterfaceData.config,
            underlayIfcName)
        val underlaySubifcConfiguration = requireNotNull(
                subinterfaceFromUnderlay.or({ subinterfaceToConfigure }),
                { "Cannot configure L2P2P on non-existent subinterface $underlayIfcName" })

        val underlaySubifcConfigurationAfter = InterfaceConfigurationBuilder(underlaySubifcConfiguration)
                .removeAugmentation(VlanSubConfigurationAugmentation::class.java)
                .setInterfaceModeNonPhysical(InterfaceModeEnum.L2Transport)
                .addAugmentation(UnderlayIfcEthernetServiceAug::class.java, underlayIfcEthernetServiceAug)
                .build()

        underlayAccess.delete(underlayIfcId)
        underlayAccess.merge(underlayIfcId, underlaySubifcConfigurationAfter)
    }

    override fun deleteCurrentAttributesWResult(
        id: InstanceIdentifier<ConnectionPoints>,
        dataBefore: ConnectionPoints,
        writeContext: WriteContext
    ): Boolean {
        if (!BasicCheck.checkData(ChecksMap.DataCheck.NetworkInstanceConfig.IID_TRANSFORMATION,
                ChecksMap.DataCheck.NetworkInstanceConfig.TYPE_L2P2P).canProcess(id, writeContext, true)) {
            return false
        }

        val l2p2InstanceName = id.firstKeyOf(NetworkInstance::class.java).name

        val (endpoint1, endpoint2) = getEndPoints(dataBefore)

        deleteEndpoint(endpoint1)
        deleteEndpoint(endpoint2)

        val underlayP2PXconnectId = L2P2PReader.UNDERLAY_P2PXCONNECT_ID.child(P2pXconnect::class.java,
                P2pXconnectKey(CiscoIosXrString(l2p2InstanceName)))
        underlayAccess.delete(underlayP2PXconnectId)
        return true
    }

    private fun deleteEndpoint(endpoint: Endpoint) {
        if (endpoint.config?.type === REMOTE::class.java) {
            return
        }

        val local = endpoint.local
        if (local.config?.subinterface != null) {
            deleteL2SubifcCofiguration(local.config?.`interface`!!, local.config?.subinterface!! as Long)
        } else {
            val ifcName = local.config?.`interface`!!
            deleteL2InterfaceConfiguration(ifcName)
        }
    }

    private fun deleteL2SubifcCofiguration(underlaySubifcName: String, index: Long) {
        val underlayIfcId = getUnderlayIfcId(underlaySubifcName + "." + index)

        val underlaySubifcConfiguration = underlayAccess.read(underlayIfcId).get()

        // If the subinterface no longer exists, don't do anything.
        // XR allows l2p2 configurations to exist on non existing subinterfaces
        if (!underlaySubifcConfiguration.isPresent)
            return

        val vlanSubConfig = VlanSubConfigurationBuilder()
                .setVlanIdentifier(VlanIdentifierBuilder()
                        .setFirstTag(VlanTag(index))
                        .setVlanType(Vlan.VlanTypeDot1q)
                        .build())
                .build()

        val vlanCfg = VlanSubConfigurationAugmentationBuilder()
                .setVlanSubConfiguration(vlanSubConfig)
                .build()

        val underlaySubifcConfigBefore = InterfaceConfigurationBuilder(underlaySubifcConfiguration.get()!!)
                .removeAugmentation(UnderlayIfcEthernetServiceAug::class.java)
                .setInterfaceModeNonPhysical(InterfaceModeEnum.Default)
                .addAugmentation(VlanSubConfigurationAugmentation::class.java, vlanCfg)
                .build()

        underlayAccess.delete(underlayIfcId)
        underlayAccess.merge(underlayIfcId, underlaySubifcConfigBefore)
    }

    override fun updateCurrentAttributesWResult(
        id: InstanceIdentifier<ConnectionPoints>,
        dataBefore: ConnectionPoints,
        dataAfter: ConnectionPoints,
        writeContext: WriteContext
    ): Boolean {
        if (!BasicCheck.checkData(ChecksMap.DataCheck.NetworkInstanceConfig.IID_TRANSFORMATION,
                ChecksMap.DataCheck.NetworkInstanceConfig.TYPE_L2P2P).canProcess(id, writeContext, false)) {
            return false
        }

        deleteCurrentAttributesWResult(id, dataBefore, writeContext)
        writeCurrentAttributesWResult(id, dataAfter, writeContext)
        return true
    }

    private fun deleteL2InterfaceConfiguration(underlayIfcName: String) {
        val underlayIfcId = getUnderlayIfcId(underlayIfcName)
        val underlayL2TransportIfcAugId = underlayIfcId.augmentation(UnderlayIfcL2TransportAug::class.java)

        underlayAccess.delete(underlayL2TransportIfcAugId.child(L2Transport::class.java))
    }

    private fun checkIfcExists(writeContext: WriteContext, ifcName: String) {
        val ifcId = IIDs.INTERFACES
                .child(Interface::class.java, InterfaceKey(ifcName))
        val data = writeContext.readAfter(ifcId)
        require(data.isPresent, { "Unknown interface $ifcName, cannot configure l2p2p" })
    }

    private fun checkSubIfcExists(writeContext: WriteContext, instanceIdentifier: InstanceIdentifier<Subinterface>):
        Subinterface {
        val subData = writeContext.readAfter(instanceIdentifier)
        val index = instanceIdentifier.firstKeyOf(Subinterface::class.java).index
        val name = instanceIdentifier.firstKeyOf(Interface::class.java).name
        require(subData.isPresent, { "Unknown subinterface $name.$index, cannot configure l2p2p" })
        return subData.get()
    }

    private fun getSubifcId(ifcName: String, subifc: Long): InstanceIdentifier<Subinterface> {
        return IIDs.INTERFACES
                .child(Interface::class.java, InterfaceKey(ifcName))
                .child(Subinterfaces::class.java)
                .child(Subinterface::class.java, SubinterfaceKey(subifc))
    }

    private fun checkIfcNoIp(writeContext: WriteContext, ifcName: String) {
        val ipSubIfcId = IIDs.INTERFACES
                .child(Interface::class.java, InterfaceKey(ifcName))
                .child(Subinterfaces::class.java)
                .child(Subinterface::class.java, SubinterfaceKey(0L))

        val data = writeContext.readAfter(ipSubIfcId)
        require(data?.orNull()?.getAugmentation(IpSubInterfaceAug::class.java) == null,
                { "Cannot configure l2p2p on ip enabled interface $ifcName" })
    }

    private fun checkVlanConfigured(writeContext: WriteContext, ifcName: String, subifc: Long) {
        val subIfcId = IIDs.INTERFACES
                .child(Interface::class.java, InterfaceKey(ifcName))
                .child(Subinterfaces::class.java)
                .child(Subinterface::class.java, SubinterfaceKey(subifc))
                .augmentation(Subinterface1::class.java)
                .child(OpenConfigVlan::class.java)
                .child(Config::class.java)

        val subData = writeContext.readAfter(subIfcId)
        require(subData.orNull()?.vlanId?.vlanId?.value?.toLong() == subifc,
                { "Subinterface $ifcName.$subifc should have configured vlan $subifc" })
    }
}