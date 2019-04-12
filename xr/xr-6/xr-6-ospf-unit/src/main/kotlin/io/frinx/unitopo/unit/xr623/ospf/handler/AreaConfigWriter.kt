/*
 * Copyright © 2019 Frinx and others.
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
package io.frinx.unitopo.unit.xr623.ospf.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance.DEFAULT_NETWORK_NAME
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.Ospf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.area.table.AreaAddresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.area.table.area.addresses.AreaAddress
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.area.table.area.addresses.AreaAddressBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.area.table.area.addresses.AreaAddressKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.area.table.area.addresses.AreaAreaId
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.area.table.area.addresses.AreaAreaIdBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.area.table.area.addresses.AreaAreaIdKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.ospf.Processes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.ospf.processes.ProcessKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.ospf.processes.process.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.ospf.processes.process.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.ospf.processes.process.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.ospf.processes.process.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.structure.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class AreaConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun updateCurrentAttributes(
        iid: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val areaId = iid.firstKeyOf(Area::class.java).identifier

        if (areaId.uint32 != null) {
            val (underlayId, underlayData) = getAreaIdData(iid)
            underlayAccess.put(underlayId, underlayData)
        } else {
            val (underlayId, underlayData) = getAreaAddressData(iid)
            underlayAccess.put(underlayId, underlayData)
        }
    }

    override fun writeCurrentAttributes(id: IID<Config>, dataAfter: Config, wtx: WriteContext) {
        val areaId = id.firstKeyOf(Area::class.java).identifier

        if (areaId.uint32 != null) {
            val (underlayId, underlayData) = getAreaIdData(id)
            underlayAccess.merge(underlayId, underlayData)
        } else {
            val (underlayId, underlayData) = getAreaAddressData(id)
            underlayAccess.merge(underlayId, underlayData)
        }
    }

    override fun deleteCurrentAttributes(id: IID<Config>, dataBefore: Config, wtx: WriteContext) {
        val (processIid, vrfName) = getIdentifiers(id)
        val areaId = id.firstKeyOf(Area::class.java).identifier

        if (areaId.uint32 != null) {
            val areaIid = getAreaIdIdentifier(processIid, vrfName, areaId.uint32.toInt())
            underlayAccess.delete(areaIid)
        } else {
            val areaIid = getAreaAddressIdentifier(processIid, vrfName, areaId.dottedQuad)
            underlayAccess.delete(areaIid)
        }
    }

    companion object {
        fun getAreaIdData(id: IID<Config>): Pair<IID<AreaAreaId>, AreaAreaId> {
            val (processIid, vrfName) = getIdentifiers(id)
            val areaId = id.firstKeyOf(Area::class.java).identifier.uint32.toInt()
            val areaIid = getAreaIdIdentifier(processIid, vrfName, areaId)

            val area = AreaAreaIdBuilder()
                    .setKey(AreaAreaIdKey(areaId))
                    .setAreaId(areaId)
                    .setRunning(true)

            return Pair(areaIid, area.build())
        }

        fun getAreaAddressData(id: IID<Config>): Pair<IID<AreaAddress>, AreaAddress> {
            val (processIid, vrfName) = getIdentifiers(id)
            val areaId = id.firstKeyOf(Area::class.java).identifier.dottedQuad
            val areaIid = getAreaAddressIdentifier(processIid, vrfName, areaId)

            val area = AreaAddressBuilder()
                    .setKey(AreaAddressKey(areaId.toIpv4NoZone()))
                    .setAddress(areaId.toIpv4NoZone())
                    .setRunning(true)

            return Pair(areaIid, area.build())
        }

        fun getAreaIdIdentifier(processIid: IID<Process>, vrfName: String, areaId: Int): IID<AreaAreaId> {
            return processIid.let {
                if (DEFAULT_NETWORK_NAME == vrfName) {
                    it.child(DefaultVrf::class.java)
                            .child(AreaAddresses::class.java)
                            .child(AreaAreaId::class.java, AreaAreaIdKey(areaId))
                } else {
                    it.child(Vrfs::class.java).child(Vrf::class.java,
                            VrfKey(CiscoIosXrString(vrfName)))
                            .child(AreaAddresses::class.java)
                            .child(AreaAreaId::class.java, AreaAreaIdKey(areaId))
                }
            }
        }

        fun getAreaAddressIdentifier(processIid: IID<Process>, vrfName: String, areaId: DottedQuad): IID<AreaAddress> {
            return processIid.let {
                if (DEFAULT_NETWORK_NAME == vrfName) {
                    it.child(DefaultVrf::class.java)
                            .child(AreaAddresses::class.java)
                            .child(AreaAddress::class.java, AreaAddressKey(areaId.toIpv4NoZone()))
                } else {
                    it.child(Vrfs::class.java).child(Vrf::class.java,
                            VrfKey(CiscoIosXrString(vrfName)))
                            .child(AreaAddresses::class.java)
                            .child(AreaAddress::class.java, AreaAddressKey(areaId.toIpv4NoZone()))
                }
            }
        }

        fun getIdentifiers(id: IID<out DataObject>): Pair<IID<Process>, String> {
            val processName = id.firstKeyOf(Protocol::class.java).name
            val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
            val processIid = IID.create(Ospf::class.java).child(Processes::class.java).child(Process::class.java,
                ProcessKey(CiscoIosXrString(processName)))
            return Pair(processIid, vrfName)
        }
    }
}

private fun DottedQuad.toIpv4NoZone() = Ipv4AddressNoZone(value)