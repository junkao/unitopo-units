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

import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.handlers.bgp.BgpWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.As
import io.frinx.unitopo.unit.xr7.bgp.IID
import io.frinx.unitopo.unit.xr7.bgp.UnderlayDefaultVrfGlobal
import io.frinx.unitopo.unit.xr7.bgp.UnderlayVrfGlobal
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.InstanceAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.InstanceAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.FourByteAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.GlobalAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.global.afs.GlobalAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.global.afs.GlobalAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.global.afs.GlobalAfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.VrfGlobalAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.vrf.global.afs.VrfGlobalAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.vrf.global.afs.VrfGlobalAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.vrf.global.afs.VrfGlobalAfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev170626.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev170626.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.afi.safi.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.Global
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.AFISAFITYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.L2VPNEVPN
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.IPV4UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class GlobalAfiSafiConfigWriter(private val underlayAccess: UnderlayAccess) : BgpWriter<Config> {

    override fun writeCurrentAttributesForType(
        id: IID<Config>,
        config: Config,
        writeContext: WriteContext
    ) {
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val asNumber = writeContext.readAfter(RWUtils.cutId(id, Global::class.java)).get().config.`as`
        val underlayAfi = requireNotNull(config.afiSafiName.toUnderlay(),
            { "Unable to configure address family: ${config.afiSafiName}. Unsupported" })

        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
            val data = GlobalAfBuilder()
                .setAfName(underlayAfi)
                .setEnable(true)
                .build()
            underlayAccess.merge(getGlobalId(asNumber, data.afName), data)
        } else {
            val data = VrfGlobalAfBuilder()
                .setAfName(underlayAfi)
                .setEnable(true)
                .build()
            underlayAccess.merge(getVrfId(vrfKey, asNumber, data.afName), data)
        }
    }

    override fun updateCurrentAttributesForType(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        // No need to update the AFI SAFI since we are just creating or deleting the it from this handler
        // no actual configuration is touched here
    }

    override fun deleteCurrentAttributesForType(
        id: IID<Config>,
        config: Config,
        writeContext: WriteContext
    ) {
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val asNumber = writeContext.readBefore(RWUtils.cutId(id, Global::class.java)).get().config.`as`
        val underlayAfi = requireNotNull(config.afiSafiName.toUnderlay(),
            { "Unable to configure address family: ${config.afiSafiName}. Unsupported" })

        if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
            underlayAccess.delete(getGlobalId(asNumber, underlayAfi))
        } else {
            underlayAccess.delete(getVrfId(vrfKey, asNumber, underlayAfi))
        }
    }

    companion object {
        fun getVrfId(vrfKey: NetworkInstanceKey, asNum: AsNumber, bgpAddressFamily: BgpAddressFamily):
            InstanceIdentifier<VrfGlobalAf> {
            val (as1, as2) = As.asToDotNotation(asNum)

            return GlobalConfigWriter.XR_BGP_ID.child(Instance::class.java,
                InstanceKey(GlobalConfigWriter.XR_BGP_INSTANCE_NAME))
                .child(InstanceAs::class.java, InstanceAsKey(BgpAsRange(as1)))
                .child(FourByteAs::class.java, FourByteAsKey(BgpAsRange(as2)))
                .child(Vrfs::class.java)
                .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfKey.name)))
                .child(UnderlayVrfGlobal::class.java)
                .child(VrfGlobalAfs::class.java)
                .child(VrfGlobalAf::class.java, VrfGlobalAfKey(bgpAddressFamily))
        }

        fun getGlobalId(asNum: AsNumber, bgpAddressFamily: BgpAddressFamily): InstanceIdentifier<GlobalAf> {
            val (as1, as2) = As.asToDotNotation(asNum)

            return GlobalConfigWriter.XR_BGP_ID.child(Instance::class.java,
                InstanceKey(GlobalConfigWriter.XR_BGP_INSTANCE_NAME))
                .child(InstanceAs::class.java, InstanceAsKey(BgpAsRange(as1)))
                .child(FourByteAs::class.java, FourByteAsKey(BgpAsRange(as2)))
                .child(DefaultVrf::class.java)
                .child(UnderlayDefaultVrfGlobal::class.java)
                .child(GlobalAfs::class.java)
                .child(GlobalAf::class.java, GlobalAfKey(bgpAddressFamily))
        }
    }
}

fun Class<out AFISAFITYPE>.toUnderlay(): BgpAddressFamily? {
    when (this) {
        L2VPNEVPN::class.java -> return BgpAddressFamily.L2vpnEvpn
        IPV4UNICAST::class.java -> return BgpAddressFamily.Ipv4Unicast
    }
    return null
}