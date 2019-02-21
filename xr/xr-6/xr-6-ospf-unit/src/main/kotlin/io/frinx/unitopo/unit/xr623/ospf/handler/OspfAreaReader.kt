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

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.handlers.ospf.OspfListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.Ospf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.area.table.AreaAddresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.ospf.Processes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.types.rev170228.OspfAreaIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.AreasBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.AreaBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.AreaKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedException
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class OspfAreaReader(private val access: UnderlayAccess)
    : OspfListReader.OspfConfigListReader<Area, AreaKey, AreaBuilder> {

    @Throws(ReadFailedException::class)
    override fun getAllIdsForType(id: IID<Area>, context: ReadContext): List<AreaKey> {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java)
        val protKey = id.firstKeyOf(Protocol::class.java)

        try {
            return getAreas(access, protKey, vrfName.name)
                    ?.let {
                        val simpleAreaIds = it.areaAreaId.orEmpty()
                                .map { AreaKey(OspfAreaIdentifier(it.areaId.toLong())) }
                                .toList()
                        val dottedQuadQreaIds = it.areaAddress.orEmpty()
                                .map { AreaKey(OspfAreaIdentifier(DottedQuad(it.address.value))) }
                                .toList()

                        simpleAreaIds + dottedQuadQreaIds
                    }.orEmpty()
        } catch (e: MdSalReadFailedException) {
            throw ReadFailedException(id, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, readData: List<Area>) {
        (builder as AreasBuilder).area = readData
    }

    override fun getBuilder(id: IID<Area>) = AreaBuilder()

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributesForType(id: IID<Area>, builder: AreaBuilder, ctx: ReadContext) {
        val key = id.firstKeyOf(Area::class.java)

        builder.identifier = key.identifier
    }

    companion object {
        private val UNDERLAY_OSPF_PROCESSES = IID.create(Ospf::class.java).child(Processes::class.java)

        fun getProcess(access: UnderlayAccess, protKey: ProtocolKey): Process? {
            return access.read(UNDERLAY_OSPF_PROCESSES)
                .checkedGet()
                .orNull()
                ?.process.orEmpty()
                .find {
                    it.processName.value == protKey.name
                }
        }

        fun getAreas(access: UnderlayAccess, protoKey: ProtocolKey, vrfName: String): AreaAddresses? {
            return getProcess(access, protoKey)
                    ?.let {
                        if (NetworInstance.DEFAULT_NETWORK_NAME == vrfName) {
                            it.defaultVrf?.areaAddresses
                        } else {
                            it.vrfs?.vrf.orEmpty()
                                    .find { it.vrfName.value == vrfName }
                                    ?.areaAddresses
                        }
                    }
        }
    }
}