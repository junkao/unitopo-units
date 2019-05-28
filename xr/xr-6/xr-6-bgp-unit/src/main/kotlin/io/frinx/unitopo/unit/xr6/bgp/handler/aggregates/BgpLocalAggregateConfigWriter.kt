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

package io.frinx.unitopo.unit.xr6.bgp.handler.aggregates

import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalAfiSafiConfigWriter
import io.frinx.unitopo.unit.xr6.bgp.handler.getAfiSafis
import io.frinx.unitopo.unit.xr6.bgp.handler.toUnderlay
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.sourced.network.table.SourcedNetworks
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.sourced.network.table.sourced.networks.SourcedNetwork
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.sourced.network.table.sourced.networks.SourcedNetworkBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.sourced.network.table.sourced.networks.SourcedNetworkKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.Bgp
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ProtocolsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpPrefix
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class BgpLocalAggregateConfigWriter(private val access: UnderlayAccess) : CompositeWriter.Child<Config> {

    override fun writeCurrentAttributesWResult(
        instanceIdentifier: InstanceIdentifier<Config>,
        config: Config,
        writeContext: WriteContext
    ): Boolean {
        if (!ChecksMap.PathCheck.Protocol.BGP.canProcess(instanceIdentifier, writeContext, false)) {
            return false
        }

        val vrfKey = instanceIdentifier.firstKeyOf(NetworkInstance::class.java)

        val networkInstance = writeContext.readAfter(RWUtils.cutId(instanceIdentifier,
            NetworkInstance::class.java).child(Protocols::class.java)).get()
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

        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
            afiSafis
                    .map { it.afiSafiName }
                    .map { it.toUnderlay() }
                    .filterNotNull()
                    .forEach { writeGlobalNetworkForAfi(it, asNumber, config.prefix) }
        } else {
            afiSafis
                    .map { it.afiSafiName }
                    .map { it.toUnderlay() }
                    .filterNotNull()
                    .forEach { writeVrfNetworkForAfi(it, vrfKey, asNumber, config.prefix) }
        }
        return true
    }

    private fun writeGlobalNetworkForAfi(it: BgpAddressFamily, asNumber: AsNumber, prefix: IpPrefix) {
        val ipPrefix = prefix.getNetAddress()

        // Check that network and address family are IP version compatible
        if (ipPrefix.ipv4Address != null && it == BgpAddressFamily.Ipv4Unicast ||
                ipPrefix.ipv6Address != null && it == BgpAddressFamily.Ipv6Unicast) {

            access.merge(GlobalAfiSafiConfigWriter.getGlobalId(asNumber, it)
                    .child(SourcedNetworks::class.java)
                    .child(SourcedNetwork::class.java, SourcedNetworkKey(ipPrefix, prefix.getNetMask())),
                    prefix.toSourcedNetwork())
        }
    }

    private fun deleteGlobalNetworkForAfi(it: BgpAddressFamily, asNumber: AsNumber, prefix: IpPrefix) {
        val ipPrefix = prefix.getNetAddress()
        if (ipPrefix.ipv4Address != null && it == BgpAddressFamily.Ipv4Unicast ||
                ipPrefix.ipv6Address != null && it == BgpAddressFamily.Ipv6Unicast) {

            access.delete(GlobalAfiSafiConfigWriter.getGlobalId(asNumber, it)
                    .child(SourcedNetworks::class.java)
                    .child(SourcedNetwork::class.java, SourcedNetworkKey(ipPrefix, prefix.getNetMask())))
        }
    }

    private fun writeVrfNetworkForAfi(
        it: BgpAddressFamily,
        vrfKey: NetworkInstanceKey,
        asNumber: AsNumber,
        prefix: IpPrefix
    ) {
        val ipPrefix = prefix.getNetAddress()

        // Check that network and address family are IP version compatible
        if (ipPrefix.ipv4Address != null && it == BgpAddressFamily.Ipv4Unicast ||
                ipPrefix.ipv6Address != null && it == BgpAddressFamily.Ipv6Unicast) {

            access.merge(GlobalAfiSafiConfigWriter.getVrfId(vrfKey, asNumber, it)
                    .child(SourcedNetworks::class.java)
                    .child(SourcedNetwork::class.java, SourcedNetworkKey(ipPrefix, prefix.getNetMask())),
                    prefix.toSourcedNetwork())
        }
    }

    private fun deleteVrfNetworkForAfi(
        it: BgpAddressFamily,
        vrfKey: NetworkInstanceKey,
        asNumber: AsNumber,
        prefix: IpPrefix
    ) {
        val ipPrefix = prefix.getNetAddress()

        if (ipPrefix.ipv4Address != null && it == BgpAddressFamily.Ipv4Unicast ||
                ipPrefix.ipv6Address != null && it == BgpAddressFamily.Ipv6Unicast) {

        access.delete(GlobalAfiSafiConfigWriter.getVrfId(vrfKey, asNumber, it)
                .child(SourcedNetworks::class.java)
                .child(SourcedNetwork::class.java, SourcedNetworkKey(ipPrefix, prefix.getNetMask())))
        }
    }

    override fun updateCurrentAttributesWResult(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ): Boolean {
        if (!ChecksMap.PathCheck.Protocol.BGP.canProcess(id, writeContext, false)) {
            return false
        }

        deleteCurrentAttributesWResult(id, dataBefore, writeContext)
        writeCurrentAttributesWResult(id, dataAfter, writeContext)
        return true
    }

    override fun deleteCurrentAttributesWResult(
        instanceIdentifier: InstanceIdentifier<Config>,
        config: Config,
        writeContext: WriteContext
    ): Boolean {
        if (!ChecksMap.PathCheck.Protocol.BGP.canProcess(instanceIdentifier, writeContext, true)) {
            return false
        }

        val vrfKey = instanceIdentifier.firstKeyOf(NetworkInstance::class.java)
        val protocolsBefore = writeContext.readBefore(RWUtils.cutId(instanceIdentifier, NetworkInstance::class.java)
            .child(Protocols::class.java)).get()
        val protocolsAfter = writeContext.readAfter(RWUtils.cutId(instanceIdentifier, NetworkInstance::class.java)
            .child(Protocols::class.java)).or(ProtocolsBuilder().build())
        val bgp = getBgpGlobal(protocolsBefore)
        val bgpAfter = getBgpGlobal(protocolsAfter)
        val asNumber = bgp!!.global?.config?.`as`
        asNumber!!
        val afiSafis = bgp.getAfiSafis()

        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
            afiSafis
                    .map { it.afiSafiName }
                    // Skip deletion of networks for AFI SAFI that's being deleted ... because XR
                    .filter { bgpAfter?.global?.afiSafis?.afiSafi.orEmpty().map { it.afiSafiName }.contains(it) }
                    .map { it.toUnderlay() }
                    .filterNotNull()
                    .forEach { deleteGlobalNetworkForAfi(it, asNumber, config.prefix) }
        } else {
            afiSafis
                    .map { it.afiSafiName }
                    // Skip deletion of networks for AFI SAFI that's being deleted ... because XR
                    .filter { bgpAfter?.global?.afiSafis?.afiSafi.orEmpty().map { it.afiSafiName }.contains(it) }
                    .map { it.toUnderlay() }
                    .filterNotNull()
                    .forEach { deleteVrfNetworkForAfi(it, vrfKey, asNumber, config.prefix) }
        }
        return true
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
    val prefixString = ipv4Prefix?.value ?: ipv6Prefix.value
    return IpAddress(prefixString.substringBefore('/').toCharArray())
}

private fun IpPrefix.getNetMask(): Int {
    val prefixString = ipv4Prefix?.value ?: ipv6Prefix.value
    return prefixString.substringAfter('/').toInt()
}

private fun IpPrefix.toSourcedNetwork(): SourcedNetwork {
    return SourcedNetworkBuilder()
            .setNetworkAddr(getNetAddress())
            .setNetworkPrefix(getNetMask())
            .build()
}