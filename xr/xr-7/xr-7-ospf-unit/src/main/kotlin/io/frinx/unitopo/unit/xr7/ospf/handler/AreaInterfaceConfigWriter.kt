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
package io.frinx.unitopo.unit.xr7.ospf.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.handlers.ospf.OspfWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.ospf.handler.AreaConfigWriter.Companion.getAreaAddressIdentifier
import io.frinx.unitopo.unit.xr7.ospf.handler.AreaConfigWriter.Companion.getAreaIdIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.area.table.area.addresses.area.content.NameScopes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.area.table.area.addresses.area.content.name.scopes.NameScope
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.area.table.area.addresses.area.content.name.scopes.NameScopeBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.area.table.area.addresses.area.content.name.scopes.NameScopeKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class AreaInterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : OspfWriter<Config> {

    override fun updateCurrentAttributesForType(
        iid: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (underlayId, underlayData) = getData(iid, dataAfter)
        underlayAccess.put(underlayId, underlayData)
    }

    override fun writeCurrentAttributesForType(id: IID<Config>, dataAfter: Config, wtx: WriteContext) {
        val (underlayId, underlayData) = getData(id, dataAfter)
        underlayAccess.merge(underlayId, underlayData)
    }

    override fun deleteCurrentAttributesForType(id: IID<Config>, dataBefore: Config, wtx: WriteContext) {
        val (processIid, vrfName) = GlobalConfigWriter.getIdentifiers(id)
        val ifaceIid = getNameScopeIdentifier(processIid, vrfName, id)

        underlayAccess.delete(ifaceIid)
    }

    private fun getData(id: IID<Config>, data: Config): Pair<IID<NameScope>, NameScope> {
        val (processIid, vrfName) = GlobalConfigWriter.getIdentifiers(id)
        val ifName = id.firstKeyOf(Interface::class.java).id
        val cost = data.metric?.value
        val ifaceIid = getNameScopeIdentifier(processIid, vrfName, id)

        val iface = NameScopeBuilder()
            .setKey(NameScopeKey(InterfaceName(ifName)))
            .let {
                if (cost != null) {
                    it.cost = cost.toLong()
                }
                it
            }

        return Pair(ifaceIid, iface.build())
    }

    companion object {
        fun getNameScopeIdentifier(processIid: IID<Process>, vrfName: String, iid: IID<Config>): IID<NameScope> {
            val areaId = iid.firstKeyOf(Area::class.java).identifier
            val ifName = iid.firstKeyOf(Interface::class.java).id

            return if (areaId.uint32 != null) {
                getAreaIdIdentifier(processIid, vrfName, areaId.uint32.toLong()).child(NameScopes::class.java)
                        .child(NameScope::class.java, NameScopeKey(InterfaceName(ifName)))
            } else {
                getAreaAddressIdentifier(processIid, vrfName, areaId.dottedQuad).child(NameScopes::class.java)
                        .child(NameScope::class.java, NameScopeKey(InterfaceName(ifName)))
            }
        }
    }
}