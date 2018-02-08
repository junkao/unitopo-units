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
import io.frinx.unitopo.unit.xr6.ospf.common.OspfWriter
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.Ospf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.Processes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.ProcessBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.ProcessKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.DefaultVrfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.VrfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.vrfs.VrfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.Config
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone
import org.opendaylight.yangtools.yang.binding.DataObject
import java.util.*
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.Config as ProtoConfig
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class GlobalConfigWriter(private val underlayAccess: UnderlayAccess) : OspfWriter<Config> {

    override fun updateCurrentAttributesForType(iid: IID<Config>, dataBefore: Config, dataAfter: Config, writeContext: WriteContext) {
        deleteCurrentAttributesForType(iid, dataBefore, writeContext)
        writeCurrentAttributesForType(iid, dataAfter, writeContext)
    }

    override fun writeCurrentAttributesForType(id: IID<Config>, dataAfter: Config, wtx: WriteContext) {
        val (underlayId, underlayCfg) = getData(id, dataAfter)

        underlayAccess.merge(underlayId, underlayCfg)
    }

    override fun deleteCurrentAttributesForType(id: IID<Config>, dataBefore: Config, wtx: WriteContext) {
        val processName = id.firstKeyOf(Protocol::class.java).name
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        val vrfIid = IID.create(Ospf::class.java).child(Processes::class.java).child(Process::class.java,
                ProcessKey(CiscoIosXrString(processName))).let {
            if (DEFAULT_VRF.equals(vrfName)) {
                it.child(DefaultVrf::class.java)
            } else {
                it.child(Vrfs::class.java).child(Vrf::class.java,
                        VrfKey(CiscoIosXrString(vrfName)))
            }
        }
        underlayAccess.delete(vrfIid)
    }

    private fun getData(id: IID<Config>, data: Config):
            Pair<IID<Process>, Process> {
        val processName = id.firstKeyOf(Protocol::class.java).name
        val routerId = data.routerId.value
        val (processIid, vrfName) = getIdentifiers(id)
        val builder = ProcessBuilder()
                .setKey(ProcessKey(CiscoIosXrString(processName)))
                .setStart(true)
                .let {
                    if (DEFAULT_VRF.equals(vrfName)) {
                        it.defaultVrf = DefaultVrfBuilder().setRouterId(Ipv4AddressNoZone(routerId)).build()
                    } else {
                        it.vrfs = VrfsBuilder()
                                .setVrf(Collections.singletonList(
                                        VrfBuilder()
                                                .setKey(VrfKey(CiscoIosXrString(vrfName)))
                                                .setVrfStart(true)
                                                .setRouterId(Ipv4AddressNoZone(routerId))
                                                .build())
                                )
                                .build()
                    }
                    it
                }

        return Pair(processIid, builder.build())
    }

    companion object {
        val DEFAULT_VRF = "default"

        public fun getIdentifiers(id: IID<out DataObject>): Pair<IID<Process>, String> {
            val processName = id.firstKeyOf(Protocol::class.java).name
            val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
            val processIid = IID.create(Ospf::class.java).child(Processes::class.java).child(Process::class.java,
                    ProcessKey(CiscoIosXrString(processName)))
            return Pair(processIid, vrfName)
        }
    }
}