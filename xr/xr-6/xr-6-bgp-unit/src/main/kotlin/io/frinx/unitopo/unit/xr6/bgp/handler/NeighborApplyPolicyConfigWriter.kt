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

package io.frinx.unitopo.unit.xr6.bgp.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.handlers.bgp.BgpWriter
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.neighbor.NeighborAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.neighbor.neighbor.afs.NeighborAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.neighbor.neighbor.afs.NeighborAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.neighbor.neighbor.afs.NeighborAfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.vrf.neighbor.VrfNeighborAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.vrf.neighbor.vrf.neighbor.afs.VrfNeighborAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.vrf.neighbor.vrf.neighbor.afs.VrfNeighborAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.vrf.neighbor.vrf.neighbor.afs.VrfNeighborAfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.apply.policy.group.apply.policy.Config
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class NeighborApplyPolicyConfigWriter(private val underlayAccess: UnderlayAccess) : BgpWriter<Config> {

    override fun updateCurrentAttributesForType(
        iid: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        deleteCurrentAttributesForType(iid, dataBefore, writeContext)
        writeCurrentAttributesForType(iid, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributesForType(iid: IID<Config>, dataBefore: Config, wc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        val bgpProcess = iid.firstKeyOf(Protocol::class.java).name.toLong()
        val neighborAddress = iid.firstKeyOf(Neighbor::class.java).neighborAddress.value
        val iid: IID<out DataObject>

        if (vrfName.equals("default")) {
            iid = getDefaultVrfNeighborAfIdentifier(bgpProcess, IpAddressNoZone(neighborAddress))
        } else {
            iid = getVrfNeighborAfIdentifier(vrfName, bgpProcess, IpAddressNoZone(neighborAddress))
        }
        try {
            underlayAccess.delete(iid)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(iid, e)
        }
    }

    override fun writeCurrentAttributesForType(iid: IID<Config>, dataAfter: Config, wc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        val bgpProcess = iid.firstKeyOf(Protocol::class.java).name.toLong()
        val neighborAddress = iid.firstKeyOf(Neighbor::class.java).neighborAddress.value
        if (vrfName.equals("default")) {
            val (iid, neighbor) = getDefautVrfNeighborAf(bgpProcess, neighborAddress, dataAfter)
            try {
                underlayAccess.merge(iid, neighbor)
            } catch (e: Exception) {
                throw io.fd.honeycomb.translate.write.WriteFailedException(iid, e)
            }
        } else {
            val (iid, neighbor) = getVrfNeighborAf(vrfName, bgpProcess, neighborAddress, dataAfter)
            try {
                underlayAccess.merge(iid, neighbor)
            } catch (e: Exception) {
                throw io.fd.honeycomb.translate.write.WriteFailedException(iid, e)
            }
        }
    }

    private fun getDefautVrfNeighborAf(bgpProcess: Long, neighbor: CharArray, data: Config):
            Pair<IID<NeighborAf>, NeighborAf> {
        val iid = getDefaultVrfNeighborAfIdentifier(bgpProcess, IpAddressNoZone(neighbor))
        val neighborAf = NeighborAfBuilder()
                .setKey(NeighborAfKey(BgpAddressFamily.Ipv4Unicast))
                .setActivate(true)
                .setRoutePolicyIn(data.importPolicy?.get(0))
                .setRoutePolicyOut(data.exportPolicy?.get(0))
                .build()
        return Pair(iid, neighborAf)
    }

    private fun getVrfNeighborAf(vrfName: String, bgpProcess: Long, neighbor: CharArray, data: Config):
            Pair<IID<VrfNeighborAf>, VrfNeighborAf> {
        val iid = getVrfNeighborAfIdentifier(vrfName, bgpProcess, IpAddressNoZone(neighbor))
        val neighborAf = VrfNeighborAfBuilder()
                .setKey(VrfNeighborAfKey(BgpAddressFamily.Ipv4Unicast))
                .setActivate(true)
                .setRoutePolicyIn(data.importPolicy?.get(0))
                .setRoutePolicyOut(data.exportPolicy?.get(0))
                .build()
        return Pair(iid, neighborAf)
    }

    companion object {
        public fun getDefaultVrfNeighborAfIdentifier(bgpProcess: Long, neighbor: IpAddressNoZone): IID<NeighborAf> {
            return NeighborConfigWriter.getDefaultVrfNeighborIdentifier(bgpProcess, neighbor)
                    .child(NeighborAfs::class.java)
                    .child(NeighborAf::class.java, NeighborAfKey(BgpAddressFamily.Ipv4Unicast))
        }

        public fun getVrfNeighborAfIdentifier(vrfName: String, bgpProcess: Long, neighbor: IpAddressNoZone):
            IID<VrfNeighborAf> {
            return NeighborConfigWriter.getVrfNeighborIdentifier(bgpProcess, vrfName, neighbor)
                    .child(VrfNeighborAfs::class.java)
                    .child(VrfNeighborAf::class.java, VrfNeighborAfKey(BgpAddressFamily.Ipv4Unicast))
        }
    }
}