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

package io.frinx.unitopo.unit.xr66.bgp.handler.aggregates

import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.As
import io.frinx.unitopo.unit.xr66.bgp.handler.BgpProtocolReader
import io.frinx.unitopo.unit.xr66.bgp.handler.getAfiSafis
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.aggregate.address.table.AggregateAddresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.aggregate.address.table.aggregate.addresses.AggregateAddress
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.aggregate.address.table.aggregate.addresses.AggregateAddressBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.aggregate.address.table.aggregate.addresses.AggregateAddressKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.InstanceAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.InstanceAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.FourByteAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfGlobal
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.VrfGlobalAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.vrf.global.afs.VrfGlobalAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.vrf.global.afs.VrfGlobalAfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev170626.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev170626.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.NiProtAggAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.Bgp
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.AFISAFITYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.IPV4UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.Aggregate
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpPrefix
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class BgpAggregateConfigWriter(private val access: UnderlayAccess) : CompositeWriter.Child<Config> {

    override fun writeCurrentAttributesWResult(
        id: IID<Config>,
        config: Config,
        writeContext: WriteContext
    ): Boolean {
        if (!ChecksMap.PathCheck.Protocol.BGP.canProcess(id, writeContext, false)) {
            return false
        }

        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val protocolKey = id.firstKeyOf(Protocol::class.java)
        val aggrKey = id.firstKeyOf(Aggregate::class.java)
        val (asNumber, afiSafis) = requires(writeContext, id)
        afiSafis.map {
            it.afiSafiName
        }.map {
            it.toUnderlay()
        }.filterNotNull().forEach {
            val underlayId = getUnderlayId(asNumber, protocolKey, vrfKey.name,
                aggrKey.prefix.getNetAddress(), aggrKey.prefix.getNetMask())
            val builder =
                aggregateBuilder(underlayId, aggrKey, config, false)
            access.merge(underlayId, builder.build())
        }
        return true
    }

    override fun updateCurrentAttributesWResult(
        id: org.opendaylight.yangtools.yang.binding.InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ): Boolean {
        if (!ChecksMap.PathCheck.Protocol.BGP.canProcess(id, writeContext, false)) {
            return false
        }

        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val protocolKey = id.firstKeyOf(Protocol::class.java)
        val aggrKey = id.firstKeyOf(Aggregate::class.java)
        val (asNumber, afiSafis) = requires(writeContext, id)
        afiSafis.map {
            it.afiSafiName
        }.map {
            it.toUnderlay()
        }.filterNotNull().forEach {
            val underlayId = getUnderlayId(asNumber, protocolKey, vrfKey.name,
                aggrKey.prefix.getNetAddress(), aggrKey.prefix.getNetMask())
            val builder = aggregateBuilder(underlayId, aggrKey, dataAfter, true)
            access.put(underlayId, builder.build())
        }
        return true
    }

    override fun deleteCurrentAttributesWResult(
        id: IID<Config>,
        config: Config,
        writeContext: WriteContext
    ): Boolean {
        if (!ChecksMap.PathCheck.Protocol.BGP.canProcess(id, writeContext, true)) {
            return false
        }

        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val protocolKey = id.firstKeyOf(Protocol::class.java)
        val aggrKey = id.firstKeyOf(Aggregate::class.java)
        val (asNumber, afiSafis) = requires(writeContext, id)
        afiSafis.map {
            it.afiSafiName
        }.map {
            it.toUnderlay()
        }.filterNotNull().forEach {
            val underlayId = getUnderlayId(asNumber, protocolKey, vrfKey.name,
                aggrKey.prefix.getNetAddress(), aggrKey.prefix.getNetMask())
            access.delete(underlayId)
        }
        return true
    }

    private fun getUnderlayId(
        asN: AsNumber,
        key: ProtocolKey,
        vrfName: String,
        addr: IpAddress,
        prefix: Int
    ): IID<AggregateAddress> {
        val (aXX, aYY) = As.asToDotNotation(asN)
        return BgpProtocolReader.UNDERLAY_BGP
            .child(Instance::class.java, InstanceKey(CiscoIosXrString(key.name)))
            .child(InstanceAs::class.java, InstanceAsKey(BgpAsRange(aXX)))
            .child(FourByteAs::class.java, FourByteAsKey(BgpAsRange(aYY)))
            .child(Vrfs::class.java)
            .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfName)))
            .child(VrfGlobal::class.java)
            .child(VrfGlobalAfs::class.java)
            .child(VrfGlobalAf::class.java, VrfGlobalAfKey(BgpAddressFamily.Ipv4Unicast))
            .child(AggregateAddresses::class.java)
            .child(AggregateAddress::class.java, AggregateAddressKey(addr, prefix))
    }

    private fun readUnderlayData(id: IID<AggregateAddress>): AggregateAddressBuilder {
        val underlayData = access.read(id).checkedGet().orNull()
        return when (underlayData) {
            null -> AggregateAddressBuilder()
            else -> AggregateAddressBuilder(underlayData)
        }
    }

    private fun aggregateBuilder(
        underlayId: IID<AggregateAddress>,
        aggrKey: AggregateKey,
        config: Config,
        update: Boolean
    ) = when (update) {
        true -> readUnderlayData(underlayId)
        else -> AggregateAddressBuilder()
    }.apply {
        aggregateAddr = aggrKey.prefix.getNetAddress()
        aggregatePrefix = aggrKey.prefix.getNetMask()
        config.getAugmentation(NiProtAggAug::class.java)?.let {
            isSummaryOnly = it.isSummaryOnly
            it.applyPolicy?.get(0)?.let {
                routePolicyName = it
            }
        }
    }

    private fun requires(context: WriteContext, id: IID<Config>): Pair<AsNumber, Set<AfiSafi>> {
        val networkInstance = context.readAfter(RWUtils.cutId(id,
            NetworkInstance::class.java).child(Protocols::class.java)).get()
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        require(vrfKey != NetworInstance.DEFAULT_NETWORK,
            { "Can't write aggregation settings in default network-insance." })

        val bgp = getBgpGlobal(networkInstance)
        requireNotNull(bgp,
            { "BGP not configured for VRF: ${vrfKey.name}. Cannot configure networks" })

        val asNumber = bgp!!.global?.config?.`as`
        requireNotNull(asNumber,
            { "BGP AS number not configured for VRF: ${vrfKey.name}. Cannot configure networks" })
        asNumber!!

        val afiSafis = bgp.getAfiSafis()
        require(afiSafis.isNotEmpty(),
            { "BGP does not contain any AFI SAFI for VRF: ${vrfKey.name}. Cannot configure networks" })

        return Pair(asNumber, afiSafis)
    }

    companion object {
        private fun getBgpGlobal(protocolsContainer: Protocols): Bgp? {
            return protocolsContainer
                .protocol.orEmpty()
                .find { protocol -> protocol.identifier == BGP::class.java }
                ?.bgp
        }
    }
}

private fun IpPrefix.getNetAddress(): IpAddress {
    val prefixString = ipv4Prefix.value
    return IpAddress(prefixString.substringBefore('/').toCharArray())
}

private fun IpPrefix.getNetMask(): Int {
    val prefixString = ipv4Prefix.value
    return prefixString.substringAfter('/').toInt()
}

private fun Class<out AFISAFITYPE>.toUnderlay(): BgpAddressFamily? {
    return when (this) {
        IPV4UNICAST::class.java -> BgpAddressFamily.Ipv4Unicast
        else -> null
    }
}