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

package io.frinx.unitopo.unit.xr6.bgp.handler.neighbor

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.handlers.bgp.BgpReader
import io.frinx.unitopo.unit.xr6.bgp.UnderlayOperBgpInstance
import io.frinx.unitopo.unit.xr6.bgp.UnderlayOperBgpInstanceKey
import io.frinx.unitopo.unit.xr6.bgp.UnderlayOperNeighbor
import io.frinx.unitopo.unit.xr6.bgp.UnderlayOperNeighborKey
import io.frinx.unitopo.unit.xr6.bgp.UnderlayOperNeighbors
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.Bgp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.BgpConnState
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827._default.vrf.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.bgp.Instances
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.bgp.instances.instance.InstanceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.vrf.table.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.vrf.table.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.vrf.table.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.BgpNeighborState
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.StateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NeighborStateReader(private val access: UnderlayAccess) : BgpReader.BgpOperReader<State, StateBuilder> {

    override fun merge(parentBuilder: Builder<out DataObject>, state: State) {
        (parentBuilder as NeighborBuilder).state = state
    }

    override fun getBuilder(p0: InstanceIdentifier<State>) = StateBuilder()

    override fun readCurrentAttributesForType(
        id: InstanceIdentifier<State>,
        builder: StateBuilder,
        readContext: ReadContext
    ) {
        val neighborKey = id.firstKeyOf(Neighbor::class.java)
        builder.neighborAddress = neighborKey.neighborAddress

        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        val data = access.read(getId(protKey, vrfKey, neighborKey))
                .checkedGet()
                .orNull()

        builder.fromUnderlay(data)
    }

    companion object {
        fun getId(protKey: ProtocolKey, vrfName: NetworkInstanceKey, neighKey: NeighborKey):
            InstanceIdentifier<UnderlayOperNeighbor> {
            return if (vrfName == NetworInstance.DEFAULT_NETWORK) {
                InstanceIdentifier.create(Bgp::class.java)
                        .child(Instances::class.java)
                        .child(UnderlayOperBgpInstance::class.java,
                            UnderlayOperBgpInstanceKey(CiscoIosXrString(protKey.name)))
                        .child(InstanceActive::class.java)
                        .child(DefaultVrf::class.java)
                        .child(UnderlayOperNeighbors::class.java)
                        .child(UnderlayOperNeighbor::class.java,
                            UnderlayOperNeighborKey(neighKey.neighborAddress.toNoZone()))
            } else {
                return InstanceIdentifier.create(Bgp::class.java)
                        .child(Instances::class.java)
                        .child(UnderlayOperBgpInstance::class.java,
                            UnderlayOperBgpInstanceKey(CiscoIosXrString(protKey.name)))
                        .child(InstanceActive::class.java)
                        .child(Vrfs::class.java)
                        .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfName.name)))
                        .child(UnderlayOperNeighbors::class.java)
                        .child(UnderlayOperNeighbor::class.java,
                            UnderlayOperNeighborKey(neighKey.neighborAddress.toNoZone()))
            }
        }
    }
}

fun IpAddress.toNoZone(): IpAddressNoZone {
    return IpAddressNoZone((ipv4Address?.value ?: ipv6Address.value).toCharArray())
}

fun StateBuilder.fromUnderlay(neighbor: UnderlayOperNeighbor?) {
    neighbor?.let {
        neighborAddress = neighbor.neighborAddress.toIp()
        isEnabled = true
        peerAs = AsNumber(neighbor.remoteAs)
        neighbor.connectionState.toOpenconfig()?.let {
            sessionState = it
        }
    }
}

fun BgpConnState.toOpenconfig(): BgpNeighborState.SessionState? {
    return when (this) {
        BgpConnState.BgpStActive -> BgpNeighborState.SessionState.ACTIVE
        BgpConnState.BgpStIdle -> BgpNeighborState.SessionState.IDLE
        BgpConnState.BgpStEstab -> BgpNeighborState.SessionState.ESTABLISHED
        BgpConnState.BgpStConnect -> BgpNeighborState.SessionState.CONNECT
        BgpConnState.BgpStOpenConfirm -> BgpNeighborState.SessionState.OPENCONFIRM
        BgpConnState.BgpStOpenSent -> BgpNeighborState.SessionState.OPENSENT

        else -> null
    }
}