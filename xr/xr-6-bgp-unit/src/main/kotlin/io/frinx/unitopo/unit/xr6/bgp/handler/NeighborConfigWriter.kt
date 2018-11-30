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
import io.frinx.unitopo.unit.network.instance.As.Companion.asToDotNotation
import io.frinx.unitopo.handlers.bgp.BgpWriter
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.Bgp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.BgpEntity
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.Neighbors
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.Neighbor
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.NeighborBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbors.NeighborKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfNeighbors
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighbor
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighborBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.neighbors.VrfNeighborKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.remote.`as`.RemoteAsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class NeighborConfigWriter(private val underlayAccess: UnderlayAccess) : BgpWriter<Config> {

    override fun updateCurrentAttributesForType(
        iid: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        deleteCurrentAttributesForType(iid, dataBefore, writeContext)
        writeCurrentAttributesForType(iid, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributesForType(iid: IID<Config>, dataBefore: Config, wtc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        val bgpProcess = iid.firstKeyOf(Protocol::class.java).name.toLong()
        val iid: IID<out DataObject>
        if (vrfName == "default") {
            iid = getDefaultVrfNeighborIdentifier(bgpProcess, IpAddressNoZone(dataBefore.neighborAddress.value))
        } else {
            iid = getDefaultVrfNeighborIdentifier(bgpProcess, IpAddressNoZone(dataBefore.neighborAddress.value))
        }
        try {
            underlayAccess.delete(iid)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(iid, e)
        }
    }

    override fun writeCurrentAttributesForType(configIid: IID<Config>, dataAfter: Config, wtc: WriteContext) {
        val vrfName = configIid.firstKeyOf(NetworkInstance::class.java).name
        val bgpProcess = configIid.firstKeyOf(Protocol::class.java).name.toLong()
        if (vrfName == "default") {
            val (iid, neighbor) = getDefautVrfNeighbor(bgpProcess, dataAfter)
            try {
                underlayAccess.merge(iid, neighbor)
            } catch (e: Exception) {
                throw io.fd.honeycomb.translate.write.WriteFailedException(iid, e)
            }
        } else {
            val (iid, neighbor) = getVrfNeighbor(bgpProcess, vrfName, dataAfter)
            try {
                underlayAccess.merge(iid, neighbor)
            } catch (e: Exception) {
                throw io.fd.honeycomb.translate.write.WriteFailedException(iid, e)
            }
        }
    }

    private fun getDefautVrfNeighbor(bgpProcess: Long, data: Config): Pair<IID<Neighbor>, Neighbor> {
        val iid = getDefaultVrfNeighborIdentifier(bgpProcess, IpAddressNoZone(data.neighborAddress.value))
        val (xx, yy) = asToDotNotation(data.peerAs)
        val neighbor = NeighborBuilder()
                .setKey(NeighborKey(IpAddressNoZone(data.neighborAddress.value)))
                .setRemoteAs(RemoteAsBuilder()
                        .setAsXx(BgpAsRange(xx))
                        .setAsYy(BgpAsRange(yy))
                        .build())
                .build()
        return Pair(iid, neighbor)
    }

    private fun getVrfNeighbor(bgpProcess: Long, vrfName: String, data: Config): Pair<IID<VrfNeighbor>, VrfNeighbor> {
        val iid = getVrfNeighborIdentifier(bgpProcess, vrfName, IpAddressNoZone(data.neighborAddress.value))
        val (xx, yy) = asToDotNotation(data.peerAs)
        val neighbor = VrfNeighborBuilder()
                .setKey(VrfNeighborKey(IpAddressNoZone(data.neighborAddress.value)))
                .setRemoteAs(RemoteAsBuilder()
                        .setAsXx(BgpAsRange(xx))
                        .setAsYy(BgpAsRange(yy))
                        .build())
                .build()
        return Pair(iid, neighbor)
    }

    companion object {
        public fun getVrfNeighborIdentifier(bgpProcess: Long, vrfName: String, neighbor: IpAddressNoZone):
            IID<VrfNeighbor> {
            return IID.create(Bgp::class.java)
                    .child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))
                    .child(InstanceAs::class.java, InstanceAsKey(BgpAsRange(0)))
                    .child(FourByteAs::class.java, FourByteAsKey(BgpAsRange(bgpProcess)))
                    .child(Vrfs::class.java)
                    .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfName)))
                    .child(VrfNeighbors::class.java)
                    .child(VrfNeighbor::class.java, VrfNeighborKey(neighbor))
        }

        public fun getDefaultVrfNeighborIdentifier(bgpProcess: Long, neighbor: IpAddressNoZone): IID<Neighbor> {
            return IID.create(Bgp::class.java)
                    .child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))
                    .child(InstanceAs::class.java, InstanceAsKey(BgpAsRange(0)))
                    .child(FourByteAs::class.java, FourByteAsKey(BgpAsRange(bgpProcess)))
                    .child(DefaultVrf::class.java)
                    .child(BgpEntity::class.java)
                    .child(Neighbors::class.java)
                    .child(Neighbor::class.java, NeighborKey(neighbor))
        }
    }
}