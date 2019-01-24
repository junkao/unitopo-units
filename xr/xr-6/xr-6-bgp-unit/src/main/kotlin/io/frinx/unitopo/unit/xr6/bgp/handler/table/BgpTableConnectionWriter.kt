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

package io.frinx.unitopo.unit.xr6.bgp.handler.table

import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.handlers.l3vrf.L3VrfWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.IID
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalAfiSafiConfigWriter
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.ospf.route.table.OspfRoutes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.ospf.route.table.ospf.routes.OspfRoute
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.ospf.route.table.ospf.routes.OspfRouteBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.ospf.route.table.ospf.routes.OspfRouteKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ProtocolsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.table.connection.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.ADDRESSFAMILY
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.IPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.IPV6
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.OSPF
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class BgpTableConnectionWriter(private val access: UnderlayAccess) : L3VrfWriter<Config> {

    override fun writeCurrentAttributesForType(
        instanceIdentifier: IID<Config>,
        config: Config,
        writeContext: WriteContext
    ) {
        if (config.dstProtocol == BGP::class.java) {

            val allProtocols = writeContext.readAfter(RWUtils.cutId(instanceIdentifier, IIDs.NE_NETWORKINSTANCE)
                .child(Protocols::class.java))
                    .or(ProtocolsBuilder().setProtocol(emptyList()).build())
                    .protocol.orEmpty()

            val dstProtocols = allProtocols
                    .filter { p -> p.identifier == BGP::class.java }

            for (dstProtocol in dstProtocols) {
                writeCurrentAttributesForBgp(instanceIdentifier, dstProtocol, config, allProtocols, true)
            }
        }
    }

    override fun updateCurrentAttributesForType(
        instanceIdentifier: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        deleteCurrentAttributesForType(instanceIdentifier, dataBefore, writeContext)
        writeCurrentAttributesForType(instanceIdentifier, dataAfter, writeContext)
    }

    private fun writeCurrentAttributesForBgp(
        id: IID<Config>,
        bgpProtocol: Protocol,
        config: Config,
        protocols: List<Protocol>,
        add: Boolean
    ) {
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        Preconditions.checkArgument(config.srcProtocol == OSPF::class.java,
                "Unable to redistribute from: %s protocol, not supported", config.srcProtocol)

        val srcProtocols = protocols
                .filter({ p -> p.identifier == config.srcProtocol })

        Preconditions.checkArgument(!srcProtocols.isEmpty(),
                "No protocols: %s configured in current network", config.srcProtocol)

        val importPolicy = config.importPolicy.orEmpty()

        Preconditions.checkArgument(importPolicy.isEmpty() || importPolicy.size == 1,
                "Only a single import policy is supported: %s", importPolicy)

        srcProtocols.forEach {

            val globalId = getId(vrfKey, bgpProtocol, config, it)

            if (add) {
                val globalData = getData(it, importPolicy)
                access.merge(globalId, globalData)
            } else {
                access.delete(globalId)
            }
        }
    }

    override fun deleteCurrentAttributesForType(
        instanceIdentifier: InstanceIdentifier<Config>,
        config: Config,
        writeContext: WriteContext
    ) {
        if (config.dstProtocol == BGP::class.java) {

            val allProtocols = writeContext.readAfter(RWUtils.cutId(instanceIdentifier, IIDs.NE_NETWORKINSTANCE)
                .child(Protocols::class.java))
                    .or(ProtocolsBuilder().setProtocol(emptyList()).build())
                    .protocol.orEmpty()

            val dstProtocols = allProtocols
                    .filter { p -> p.identifier == BGP::class.java }

            for (dstProtocol in dstProtocols) {
                writeCurrentAttributesForBgp(instanceIdentifier, dstProtocol, config, allProtocols, true)
            }
        }
    }

    companion object {

        private fun getId(vrfKey: NetworkInstanceKey, bgpProtocol: Protocol, config: Config, it: Protocol):
            InstanceIdentifier<OspfRoute> {
            return if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
                getGlobalId(bgpProtocol, config, it)
            } else {
                getVrfId(vrfKey, bgpProtocol, config, it)
            }
        }

        private fun getGlobalId(bgpProtocol: Protocol, config: Config, srcProto: Protocol):
            InstanceIdentifier<OspfRoute> {
            val bgpAs = bgpProtocol.bgp.global.config.`as`
            val afi = config.addressFamily.toUnderlay()
            requireNotNull(afi, { "Unsupported redistribution address family: ${config.addressFamily}" })

            return GlobalAfiSafiConfigWriter.getGlobalId(bgpAs, afi!!)
                    .child(OspfRoutes::class.java)
                    .child(OspfRoute::class.java, OspfRouteKey(CiscoIosXrString(srcProto.name)))
        }

        private fun getVrfId(vrfKey: NetworkInstanceKey, bgpProtocol: Protocol, config: Config, srcProto: Protocol):
            InstanceIdentifier<OspfRoute> {
            val bgpAs = bgpProtocol.bgp.global.config.`as`
            val afi = config.addressFamily.toUnderlay()
            requireNotNull(afi, { "Unsupported redistribution address family: ${config.addressFamily}" })

            return GlobalAfiSafiConfigWriter.getVrfId(vrfKey, bgpAs, afi!!)
                    .child(OspfRoutes::class.java)
                    .child(OspfRoute::class.java, OspfRouteKey(CiscoIosXrString(srcProto.name)))
        }

        private fun getData(srcProto: Protocol, importPolicy: List<String>): OspfRoute? {
            return OspfRouteBuilder()
                    .setInstanceName(CiscoIosXrString(srcProto.name))
                    .setRoutePolicyName(importPolicy.firstOrNull())
                    .build()
        }
    }
}

public fun Class<out ADDRESSFAMILY>.toUnderlay(): BgpAddressFamily? {
    return when (this) {
        IPV4::class.java -> BgpAddressFamily.Ipv4Unicast
        IPV6::class.java -> BgpAddressFamily.Ipv6Unicast
        else -> null
    }
}