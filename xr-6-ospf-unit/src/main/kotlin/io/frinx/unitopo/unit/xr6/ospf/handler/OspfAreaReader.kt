/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package io.frinx.unitopo.unit.xr6.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.ospf.common.OspfListReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.AreaAddresses
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospf.types.rev170228.OspfAreaIdentifier
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.AreasBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.AreaBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.AreaKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedException
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfAreaReader(private val access: UnderlayAccess) : OspfListReader.OspfConfigListReader<Area, AreaKey, AreaBuilder> {

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
        val protKey = id.firstKeyOf(Protocol::class.java)
        val key = id.firstKeyOf(Area::class.java)

        builder.identifier = key.identifier
    }

    companion object {

        fun getAreas(access: UnderlayAccess, protoKey: ProtocolKey, vrfName: String): AreaAddresses? {
            return GlobalConfigReader.getProcess(access, protoKey)
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
