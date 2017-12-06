/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.common.BgpAsConverter
import io.frinx.unitopo.unit.xr6.bgp.common.BgpWriter
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
import java.util.regex.Pattern

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class NeighborConfigWriter(private val underlayAccess: UnderlayAccess) : BgpWriter<Config> {

    override fun updateCurrentAttributesForType(iid: IID<Config>, dataBefore: Config, dataAfter: Config, writeContext: WriteContext) {
        deleteCurrentAttributesForType(iid, dataBefore, writeContext)
        writeCurrentAttributesForType(iid, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributesForType(iid: IID<Config>, dataBefore: Config, wtc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        val bgpProcess = iid.firstKeyOf(Protocol::class.java).name.toLong()
        val iid: IID<out DataObject>
        if(vrfName.equals("default")) {
            iid = getDefaultVrfNeighborIdentifier(bgpProcess,IpAddressNoZone(dataBefore.neighborAddress.value))
        } else {
            iid = getDefaultVrfNeighborIdentifier(bgpProcess,IpAddressNoZone(dataBefore.neighborAddress.value))
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
        if (vrfName.equals("default")) {
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
        val (xx, yy) = BgpAsConverter.longToXxYy(data.peerAs.value)
        val neighbor = NeighborBuilder()
                .setKey(NeighborKey(IpAddressNoZone(data.neighborAddress.value)))
                .setRemoteAs(RemoteAsBuilder()
                        .setAsXx(BgpAsRange(xx as Long))
                        .setAsYy(BgpAsRange(yy as Long))
                        .build())
                .build()
        return Pair(iid, neighbor)
    }

    private fun getVrfNeighbor(bgpProcess: Long, vrfName: String, data: Config): Pair<IID<VrfNeighbor>, VrfNeighbor> {
        val iid = getVrfNeighborIdentifier(bgpProcess, vrfName, IpAddressNoZone(data.neighborAddress.value))
        val (xx, yy) = BgpAsConverter.longToXxYy(data.peerAs.value)
        val neighbor = VrfNeighborBuilder()
                .setKey(VrfNeighborKey(IpAddressNoZone(data.neighborAddress.value)))
                .setRemoteAs(RemoteAsBuilder()
                        .setAsXx(BgpAsRange(xx as Long))
                        .setAsYy(BgpAsRange(yy as Long))
                        .build())
                .build()
        return Pair(iid, neighbor)
    }

    companion object {
        public fun getVrfNeighborIdentifier(bgpProcess: Long, vrfName: String, neighbor: IpAddressNoZone): IID<VrfNeighbor> {
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