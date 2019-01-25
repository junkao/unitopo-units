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

package io.frinx.unitopo.unit.xr6.network.instance.vrf

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.translate.unit.commons.handler.spi.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.As
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import io.frinx.unitopo.unit.xr6.bgp.handler.GlobalConfigWriter.Companion.XR_BGP_INSTANCE_NAME
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.VrfAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.af.table.afs.Af
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.BgpRouteDistinguisher
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L3VRF
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.RouteDistinguisher
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.ADDRESSFAMILY
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.IPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.IPV6
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.RouteDistinguisher as UnderlayRouteDistinguisher

class VrfConfigReader(private val underlayAccess: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder>,
        CompositeReader.Child<Config, ConfigBuilder> {

    override fun readCurrentAttributes(id: InstanceIdentifier<Config>, builder: ConfigBuilder, ctx: ReadContext) {
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        if (isVrf(id)) {
            val rd = getRd(underlayAccess, vrfKey)

            underlayAccess.read(VrfReader.VRFS_ID.child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfKey.name))))
                    .checkedGet()
                    .orNull()
                    ?.let { builder.fromUnderlay(it, rd) }
        }
    }

    @Throws(ReadFailedException::class)
    private fun isVrf(id: InstanceIdentifier<Config>): Boolean {
        return VrfReader.getAllIds(underlayAccess).contains(id.firstKeyOf<NetworkInstance,
            NetworkInstanceKey>(NetworkInstance::class.java))
    }

    override fun getBuilder(id: InstanceIdentifier<Config>): ConfigBuilder = ConfigBuilder()

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as NetworkInstanceBuilder).config = readValue
    }

    companion object {

        /**
         * Get RD from BGP configuration
         */
        private fun getRd(underlayAccess: UnderlayAccess, vrfKey: NetworkInstanceKey): RouteDistinguisher? {
            val data = underlayAccess.read(BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java,
                InstanceKey(XR_BGP_INSTANCE_NAME)))
                    .checkedGet()
                    .orNull()
            return BgpProtocolReader.getFirst4ByteAs(data)
                    ?.vrfs
                    ?.vrf.orEmpty()
                    .find { it.vrfName.value == vrfKey.name }
                    ?.vrfGlobal
                    ?.routeDistinguisher
                    ?.toOpenconfig()
        }
    }
}

private fun UnderlayRouteDistinguisher.toOpenconfig(): RouteDistinguisher {
    return when (this.type) {
        BgpRouteDistinguisher.Auto -> RouteDistinguisher("auto")
        BgpRouteDistinguisher.Ipv4Address -> RouteDistinguisher("${address.value}:${addressIndex.value}")
        BgpRouteDistinguisher.As -> RouteDistinguisher("${`as`.value}:${asIndex.value}")
        BgpRouteDistinguisher.FourByteAs -> {
            val asFromDotNotation = As.asFromDotNotation(asXx.value, `as`.value)
            RouteDistinguisher("${asFromDotNotation.value}:$asIndex")
        }
        else -> {
            throw IllegalArgumentException("Unable to parse rd: $this, Unsupported format")
        }
    }
}

private fun ConfigBuilder.fromUnderlay(underlay: Vrf, rd: RouteDistinguisher?) {
    name = underlay.vrfName.value
    description = underlay.description
    type = L3VRF::class.java
    rd?.let {
        routeDistinguisher = rd
    }

    enabledAddressFamilies = underlay.afs?.af.orEmpty().mapNotNull { it.toOpenconfig() }
}

fun Af.toOpenconfig(): Class<out ADDRESSFAMILY>? {
    return when (afName) {
        VrfAddressFamily.Ipv4 -> IPV4::class.java
        VrfAddressFamily.Ipv6 -> IPV6::class.java
        else -> null
    }
}