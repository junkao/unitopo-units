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

package io.frinx.unitopo.unit.xr7.bgp.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.handlers.bgp.BgpListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.global.afs.GlobalAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.vrf.global.afs.VrfGlobalAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev170626.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.AfiSafisBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.AFISAFITYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.IPV4UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.L2VPNEVPN
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class GlobalAfiSafiReader(private val access: UnderlayAccess) :
    BgpListReader.BgpConfigListReader<AfiSafi, AfiSafiKey, AfiSafiBuilder> {

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

    override fun readCurrentAttributesForType(
        id: InstanceIdentifier<AfiSafi>,
        afiSafiBuilder: AfiSafiBuilder,
        readContext: ReadContext
    ) {
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

fun BgpAddressFamily.toOpenconfig(): Class<out AFISAFITYPE>? {
    when (this) {
        BgpAddressFamily.L2vpnEvpn -> return L2VPNEVPN::class.java
        BgpAddressFamily.Ipv4Unicast -> return IPV4UNICAST::class.java
    }
    return null
}