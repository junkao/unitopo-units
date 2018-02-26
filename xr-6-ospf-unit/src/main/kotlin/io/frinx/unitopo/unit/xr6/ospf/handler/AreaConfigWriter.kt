/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.frinx.unitopo.unit.xr6.ospf.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.handlers.ospf.OspfWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.AreaAddresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.area.addresses.AreaAreaId
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.area.addresses.AreaAreaIdBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.area.addresses.AreaAreaIdKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.structure.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class AreaConfigWriter(private val underlayAccess: UnderlayAccess) : OspfWriter<Config> {

    override fun updateCurrentAttributesForType(iid: IID<Config>, dataBefore: Config, dataAfter: Config, writeContext: WriteContext) {
        deleteCurrentAttributesForType(iid, dataBefore, writeContext)
        writeCurrentAttributesForType(iid, dataAfter, writeContext)
    }

    override fun writeCurrentAttributesForType(id: IID<Config>, dataAfter: Config, wtx: WriteContext) {
        val (underlayId, underlayData) = getData(id)

        try {
            underlayAccess.merge(underlayId, underlayData)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributesForType(id: IID<Config>, dataBefore: Config, wtx: WriteContext) {
        val (processIid, vrfName) = GlobalConfigWriter.getIdentifiers(id)
        val areaId = id.firstKeyOf(Area::class.java).identifier.uint32.toInt()
        val areaIid = getAreaIdentifier(processIid, vrfName, areaId)

        try {
            underlayAccess.delete(areaIid)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(id, e)
        }
    }

    private fun getData(id: IID<Config>): Pair<IID<AreaAreaId>, AreaAreaId> {
        val (processIid, vrfName) = GlobalConfigWriter.getIdentifiers(id)
        val areaId = id.firstKeyOf(Area::class.java).identifier.uint32.toInt()
        val areaIid = getAreaIdentifier(processIid, vrfName, areaId)

        val area = AreaAreaIdBuilder()
                .setKey(AreaAreaIdKey(areaId))
                .setAreaId(areaId)
                .setRunning(true)

        return Pair(areaIid, area.build())
    }

    companion object {
        fun getAreaIdentifier(processIid: IID<Process>, vrfName: String, areaId: Int): IID<AreaAreaId> {
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
