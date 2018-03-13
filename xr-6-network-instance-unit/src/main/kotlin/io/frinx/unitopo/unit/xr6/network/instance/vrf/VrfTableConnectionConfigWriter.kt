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

package io.frinx.unitopo.unit.xr6.network.instance.vrf

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.Bgp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfGlobal
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.VrfGlobalAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.vrf.global.afs.VrfGlobalAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.vrf.global.afs.VrfGlobalAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.vrf.global.afs.VrfGlobalAfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.ospf.route.table.OspfRoutesBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.ospf.route.table.ospf.routes.OspfRouteBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.Ospf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.OspfRedistLsa
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.OspfRedistProtocol
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.Processes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.ProcessKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.Vrfs as OspfVrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.vrfs.Vrf as OspfVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.vrfs.VrfKey as OspfVrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.Redistribution
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.RedistributionBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.redistribution.RedistributesBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.redistribution.redistributes.RedistributeBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.redistribution.redistributes.RedistributeKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.redistribution.redistributes.redistribute.BgpBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.redistribution.redistributes.redistribute.BgpKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.table.connection.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.INSTALLPROTOCOLTYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.OSPF
import java.util.*
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class VrfTableConnectionConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun deleteCurrentAttributes(iid: IID<Config>, data: Config, wtc: WriteContext) {
    }

    override fun writeCurrentAttributes(iid: IID<Config>, data: Config, wtc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        val protocols = wtc.readAfter(IID.create(NetworkInstances::class.java)
                .child(NetworkInstance::class.java, NetworkInstanceKey(vrfName))
                .child(Protocols::class.java)).get()

        val toProto = data.dstProtocol

        var toProtoName = ""
        for (protocol: Protocol in protocols.protocol!!) {
            if(protocol.identifier.name.equals(toProto.name)) {
                toProtoName = protocol.name
                break
            }
        }

        val fromProto = data.srcProtocol
        var fromProtoName = ""
        for (protocol: Protocol in protocols.protocol!!) {
            if(protocol.identifier.name.equals(fromProto.name)) {
                fromProtoName = protocol.name
                break
            }
        }

        if(toProto == BGP::class.java){
            val (bgpIid, bgpData) =getBgpData(toProtoName, fromProto, fromProtoName, vrfName)
            try {
                underlayAccess.merge(bgpIid, bgpData)
            } catch (e: Exception) {
                throw io.fd.honeycomb.translate.write.WriteFailedException(bgpIid, e)
            }
        }

        if(toProto == OSPF::class.java){
            val (ospfIid, ospfData) =getOspfData(toProtoName, fromProto, fromProtoName, vrfName)
            try {
                underlayAccess.merge(ospfIid, ospfData)
            } catch (e: Exception) {
                throw io.fd.honeycomb.translate.write.WriteFailedException(ospfIid, e)
            }
        }
    }

    private fun getBgpData(bgpAs: String, fromProto: Class<out INSTALLPROTOCOLTYPE>?, fromProtoName: String, vrfName: String):
        Pair<IID<VrfGlobalAf>, VrfGlobalAf> {
        if(fromProto == OSPF::class.java) {
            val vrfGlobalAfIid = getVrfGlobalAfIid(bgpAs.toLong(), vrfName)
            val vrfGlobalAf = VrfGlobalAfBuilder()
                    .setKey(VrfGlobalAfKey(BgpAddressFamily.Ipv4Unicast))
                    .setEnable(true)
                    .setOspfRoutes(OspfRoutesBuilder()
                            .setOspfRoute(Arrays.asList(OspfRouteBuilder()
                                    .setInstanceName(CiscoIosXrString(fromProtoName))
                                    .build()))
                            .build())
                    .build()
            return Pair(vrfGlobalAfIid, vrfGlobalAf)
        } else {
            TODO("Redistribution from other protocols than OSPF not implemented")
        }
    }

    private fun getOspfData(ospfProcess: String, fromProto: Class<out INSTALLPROTOCOLTYPE>?, fromProtoName: String, vrfName: String):
            Pair<IID<Redistribution>, Redistribution> {
        if(fromProto == BGP::class.java) {
            val redistributionIid = getRedistributionIid(ospfProcess, vrfName)
            val redistribution = RedistributionBuilder()
                    .setRedistributes(RedistributesBuilder()
                            .setRedistribute(Arrays.asList(RedistributeBuilder()
                                    .setKey(RedistributeKey(OspfRedistProtocol.Bgp))
                                    .setBgp(Arrays.asList(BgpBuilder()
                                            .setKey(BgpKey(0,fromProtoName.toLong(),CiscoIosXrString("bgp")))
                                            .setClassful(false)
                                            .setOspfRedistLsaType(OspfRedistLsa.External)
                                            .build()))
                                    .build()))
                            .build())
                    .build()
            return Pair(redistributionIid, redistribution)
        } else {
            TODO("Redistribution from other protocols than BGP not implemented")
        }
    }

    companion object {
        public fun getVrfGlobalAfIid(gbpAs: Long, vrfName: String): IID<VrfGlobalAf> {
            return IID.create(Bgp::class.java)
                    .child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))
                    .child(InstanceAs::class.java, InstanceAsKey(BgpAsRange(0)))
                    .child(FourByteAs::class.java, FourByteAsKey(BgpAsRange(gbpAs)))
                    .child(Vrfs::class.java)
                    .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfName)))
                    .child(VrfGlobal::class.java)
                    .child(VrfGlobalAfs::class.java)
                    .child(VrfGlobalAf::class.java, VrfGlobalAfKey(BgpAddressFamily.Ipv4Unicast))
        }

        public fun getRedistributionIid(ospfProcess: String, vrfName: String): IID<Redistribution> {
            return IID.create(Ospf::class.java)
                    .child(Processes::class.java)
                    .child(Process::class.java, ProcessKey(CiscoIosXrString(ospfProcess)))
                    .child(OspfVrfs::class.java)
                    .child(OspfVrf::class.java, OspfVrfKey(CiscoIosXrString(vrfName)))
                    .child(Redistribution::class.java)
        }
    }
}