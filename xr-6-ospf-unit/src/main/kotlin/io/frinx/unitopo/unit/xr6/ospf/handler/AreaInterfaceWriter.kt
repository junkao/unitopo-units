/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.frinx.unitopo.unit.xr6.ospf.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.ospf.common.OspfListWriter
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.AreaAddresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.area.addresses.AreaAreaId
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.area.addresses.AreaAreaIdKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.area.addresses.area.content.NameScopes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.area.addresses.area.content.name.scopes.NameScope
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.area.addresses.area.content.name.scopes.NameScopeBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.area.addresses.area.content.name.scopes.NameScopeKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class AreaInterfaceWriter(private val underlayAccess: UnderlayAccess) : OspfListWriter<Interface, InterfaceKey> {

    override fun updateCurrentAttributesForType(iid: IID<Interface>, dataBefore: Interface, dataAfter: Interface, writeContext: WriteContext) {
        deleteCurrentAttributesForType(iid, dataBefore, writeContext)
        writeCurrentAttributesForType(iid, dataAfter, writeContext)
    }

    override fun writeCurrentAttributesForType(id: IID<Interface>, dataAfter: Interface, wtx: WriteContext) {
        val (underlayId, underlayData) = getData(id, dataAfter)

        try {
            underlayAccess.merge(underlayId, underlayData)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributesForType(id: IID<Interface>, dataBefore: Interface, wtx: WriteContext) {
        val (processIid, vrfName) = GlobalConfigWriter.getIdentifiers(id)
        val ifaceIid = getNameScopeIdentifier(processIid, vrfName, id)

        try {
            underlayAccess.delete(ifaceIid)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(id, e)
        }
    }

    private fun getData(id: IID<Interface>, data: Interface): Pair<IID<NameScope>, NameScope> {
        val (processIid, vrfName) = GlobalConfigWriter.getIdentifiers(id)
        val ifName = id.firstKeyOf(Interface::class.java).id
        val cost = data.config?.metric?.value
        val ifaceIid = getNameScopeIdentifier(processIid, vrfName, id)

        val iface = NameScopeBuilder()
                .setKey(NameScopeKey(InterfaceName(ifName)))
                .setRunning(true).let {
                    if (cost != null) {
                        it.cost = cost.toLong()
                    }
                    it
                }

        return Pair(ifaceIid, iface.build())
    }

    companion object {
        public fun getNameScopeIdentifier(processIid: IID<Process>, vrfName: String, iid: IID<Interface>): IID<NameScope> {
            val areaId = iid.firstKeyOf(Area::class.java).identifier.uint32.toInt()
            val ifName = iid.firstKeyOf(Interface::class.java).id
            return getAreaIdIdentifier(processIid, vrfName, areaId).child(NameScopes::class.java)
                    .child(NameScope::class.java, NameScopeKey(InterfaceName(ifName)))
        }

        public fun getAreaIdIdentifier(processIid: IID<Process>, vrfName: String, areaId: Int)
            :IID<AreaAreaId>{
            return processIid.let {
                if (GlobalConfigWriter.DEFAULT_VRF.equals(vrfName)) {
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
    }
}