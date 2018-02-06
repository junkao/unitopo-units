/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.network.instance.protocol.bgp.common.BgpListReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.global.afs.GlobalAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.vrf.global.afs.VrfGlobalAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.AfiSafisBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.*
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class GlobalAfiSafiReader(private val access: UnderlayAccess) : BgpListReader.BgpConfigListReader<AfiSafi, AfiSafiKey, AfiSafiBuilder> {

    override fun getAllIdsForType(id: InstanceIdentifier<AfiSafi>, readContext: ReadContext): List<AfiSafiKey> {
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        val data = access.read(BgpProtocolReader.UNDERLAY_BGP
                .child(Instance::class.java, InstanceKey(CiscoIosXrString(protKey.name))))
                .checkedGet()
                .orNull()

        return parseAfiSafi(data, vrfKey)
    }

    override fun merge(builder: Builder<out DataObject>, list: List<AfiSafi>) {
        (builder as AfiSafisBuilder).afiSafi = list
    }

    override fun readCurrentAttributesForType(id: InstanceIdentifier<AfiSafi>, afiSafiBuilder: AfiSafiBuilder, readContext: ReadContext) {
        afiSafiBuilder.afiSafiName = id.firstKeyOf(AfiSafi::class.java).afiSafiName
    }

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<AfiSafi>) = AfiSafiBuilder()

    companion object {
        fun parseAfiSafi(data: Instance?, vrfKey: NetworkInstanceKey): List<AfiSafiKey> {
            val fourByteAs = BgpProtocolReader.getFirst4ByteAs(data)

            val afs = if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
                getGlobalAfs(fourByteAs)
                        .map { it.afName }
            } else {
                getVrfAfs(fourByteAs, vrfKey)
                        .map { it.afName }
            }

            return afs
                    .map { it.toOpenconfig() }
                    .filterNotNull()
                    .map { AfiSafiKey(it) }
                    .toList()
        }

        fun getVrfAfs(fourByteAs: FourByteAs?, vrfKey: NetworkInstanceKey): List<VrfGlobalAf> {
            return fourByteAs
                    ?.vrfs
                    ?.vrf.orEmpty()
                    .find { it.vrfName.value == vrfKey.name }
                    ?.vrfGlobal
                    ?.vrfGlobalAfs
                    ?.vrfGlobalAf.orEmpty()
        }

        fun getGlobalAfs(fourByteAs: FourByteAs?): List<GlobalAf> {
            return fourByteAs
                    ?.defaultVrf
                    ?.global
                    ?.globalAfs
                    ?.globalAf.orEmpty()
        }
    }
}

public fun BgpAddressFamily.toOpenconfig(): Class<out AFISAFITYPE>? {
    when (this) {
        BgpAddressFamily.Ipv4Unicast -> return IPV4UNICAST::class.java
        BgpAddressFamily.VpNv4Unicast -> return L3VPNIPV4UNICAST::class.java
        BgpAddressFamily.VpNv6Unicast -> return L3VPNIPV6UNICAST::class.java
        BgpAddressFamily.Ipv6Unicast -> return IPV6UNICAST::class.java
    }

    return null
}
