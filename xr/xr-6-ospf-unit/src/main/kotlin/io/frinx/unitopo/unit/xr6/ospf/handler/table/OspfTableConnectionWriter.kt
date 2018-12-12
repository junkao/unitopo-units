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

package io.frinx.unitopo.unit.xr6.ospf.handler.table

import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.handlers.l3vrf.L3VrfWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.As
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.Ospf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.OspfRedistLsa
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.OspfRedistProtocol
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.Processes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.ProcessKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.Redistribution
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.redistribution.Redistributes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.redistribution.redistributes.Redistribute
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.redistribution.redistributes.RedistributeBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.redistribution.redistributes.RedistributeKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.redistribution.redistributes.redistribute.ApplicationOrIsisOrOspfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.redistribution.redistributes.redistribute.Bgp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.redistribution.redistributes.redistribute.BgpBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.redistribution.redistribution.redistributes.redistribute.BgpKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ProtocolsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.table.connection.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.IPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.INSTALLPROTOCOLTYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.OSPF
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class OspfTableConnectionWriter(private val access: UnderlayAccess) : L3VrfWriter<Config> {

    override fun updateCurrentAttributesForType(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        deleteCurrentAttributesForType(id, dataBefore, writeContext)
        writeCurrentAttributesForType(id, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributesForType(
        instanceIdentifier: InstanceIdentifier<Config>,
        config: Config,
        writeContext: WriteContext
    ) {
        if (config.dstProtocol == OSPF::class.java) {
            val allProtocols = writeContext.readBefore(RWUtils.cutId(instanceIdentifier, IIDs.NE_NETWORKINSTANCE)
                .child(Protocols::class.java))
                    .or(ProtocolsBuilder().setProtocol(emptyList()).build())
                    .protocol.orEmpty()

            allProtocols
                    .filter { it.identifier == OSPF::class.java }
                    .forEach { writeCurrentAttributesForOspf(instanceIdentifier, config, it, allProtocols, false) }
        }
    }

    override fun writeCurrentAttributesForType(
        instanceIdentifier: InstanceIdentifier<Config>,
        config: Config,
        writeContext: WriteContext
    ) {
        if (config.dstProtocol == OSPF::class.java) {

            val allProtocols = writeContext.readAfter(RWUtils.cutId(instanceIdentifier, IIDs.NE_NETWORKINSTANCE)
                .child(Protocols::class.java))
                    .or(ProtocolsBuilder().setProtocol(emptyList()).build())
                    .protocol.orEmpty()

            allProtocols
                    .filter { it.identifier == OSPF::class.java }
                    .forEach { writeCurrentAttributesForOspf(instanceIdentifier, config, it, allProtocols, true) }
        }
    }

    private fun writeCurrentAttributesForOspf(
        id: InstanceIdentifier<Config>,
        config: Config,
        dstProtocol: Protocol,
        protocols: List<Protocol>,
        add: Boolean
    ) {
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        val srcProtocols = protocols
                .filter { p -> p.identifier == config.srcProtocol }

        require(!srcProtocols.isEmpty(),
                { "No protocols: ${config.srcProtocol} configured in current network" })

        val importPolicy = config.importPolicy.orEmpty()

        require(importPolicy.isEmpty() || importPolicy.size == 1,
                { "Only a single import policy is supported: $importPolicy" })

        srcProtocols.forEach {

            val globalId = getId(config, dstProtocol, vrfKey)

            if (add) {
                val globalData = getData(it, importPolicy)
                access.merge(globalId, globalData)
            } else {
                if (it.identifier == BGP::class.java && it.bgp?.global?.config?.`as` != null) {
                    val (asXX, asYY) = As.asToDotNotation(it.bgp.global.config.`as`)
                    access.delete(globalId.child(Bgp::class.java, BgpKey(asXX, asYY, CiscoIosXrString("bgp"))))
                } else {
                    access.delete(globalId)
                }
            }
        }
    }

    companion object {

        private fun getId(
            config: Config,
            dstProtocol: Protocol,
            vrfKey: NetworkInstanceKey
        ): InstanceIdentifier<Redistribute> {
            return if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
                getGlobalId(dstProtocol, config)
            } else {
                getVrfId(vrfKey, dstProtocol, config)
            }
        }

        private fun getGlobalId(dstProtocol: Protocol, config: Config): InstanceIdentifier<Redistribute> {
            requireNotNull(config.addressFamily == IPV4::class.java,
                    { "Unsupported redistribution address family: ${config.addressFamily}" })

            val srcProtocol = config.srcProtocol.toUnderlay()
            requireNotNull(srcProtocol,
                    { "Unsupported source protocol type: ${config.srcProtocol}" })

            return InstanceIdentifier.create(Ospf::class.java)
                    .child(Processes::class.java)
                    .child(Process::class.java, ProcessKey(CiscoIosXrString(dstProtocol.name)))
                    .child(DefaultVrf::class.java)
                    .child(Redistribution::class.java)
                    .child(Redistributes::class.java)
                    .child(Redistribute::class.java, RedistributeKey(srcProtocol))
        }

        private fun getVrfId(
            vrfKey: NetworkInstanceKey,
            dstProtocol: Protocol,
            config: Config
        ): InstanceIdentifier<Redistribute> {
            requireNotNull(config.addressFamily == IPV4::class.java,
                    { "Unsupported redistribution address family: ${config.addressFamily}" })

            val srcProtocol = config.srcProtocol.toUnderlay()
            requireNotNull(srcProtocol,
                    { "Unsupported source protocol type: ${config.srcProtocol}" })

            return InstanceIdentifier.create(Ospf::class.java)
                    .child(Processes::class.java)
                    .child(Process::class.java, ProcessKey(CiscoIosXrString(dstProtocol.name)))
                    .child(Vrfs::class.java)
                    .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfKey.name)))
                    .child(Redistribution::class.java)
                    .child(Redistributes::class.java)
                    .child(Redistribute::class.java, RedistributeKey(srcProtocol))
        }

        private fun getData(srcProto: Protocol, importPolicy: List<String>): Redistribute? {
            val srcProtocolType = srcProto.identifier.toUnderlay()
            return when (srcProtocolType) {
                OspfRedistProtocol.Ospf -> RedistributeBuilder()
                        .setProtocolName(srcProtocolType)
                        .setApplicationOrIsisOrOspf(listOf(ApplicationOrIsisOrOspfBuilder()
                                .setRoutePolicyName(importPolicy.firstOrNull())
                                .setInstanceName(CiscoIosXrString(srcProto.name))
                                .build()
                        )).build()
                OspfRedistProtocol.Bgp -> RedistributeBuilder()
                        .setProtocolName(srcProtocolType)
                        .setBgp(listOf(BgpBuilder()
                                .setInstanceName(CiscoIosXrString("bgp"))
                                .setAsXx(As.asToDotNotation(srcProto.bgp.global.config.`as`).first)
                                .setAsYy(As.asToDotNotation(srcProto.bgp.global.config.`as`).second)
                                .setClassful(false)
                                .setOspfRedistLsaType(OspfRedistLsa.External)
                                .setRoutePolicyName(importPolicy.firstOrNull())
                                .build()))
                        .build()
                else -> null
            }
        }
    }
}

private fun Class<out INSTALLPROTOCOLTYPE>.toUnderlay(): OspfRedistProtocol? {
    return when (this) {
        OSPF::class.java -> OspfRedistProtocol.Ospf
        BGP::class.java -> OspfRedistProtocol.Bgp
        else -> null
    }
}