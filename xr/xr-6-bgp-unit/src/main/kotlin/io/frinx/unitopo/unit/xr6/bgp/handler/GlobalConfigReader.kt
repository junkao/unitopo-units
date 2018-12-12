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

package io.frinx.unitopo.unit.xr6.bgp.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.As.Companion.asFromDotNotation
import io.frinx.unitopo.handlers.bgp.BgpReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.Bgp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.yang.rev170403.DottedQuad
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class GlobalConfigReader(private val access: UnderlayAccess) : BgpReader.BgpConfigReader<Config, ConfigBuilder> {

    override fun getBuilder(id: InstanceIdentifier<Config>) = ConfigBuilder()

    override fun readCurrentAttributesForType(
        id: InstanceIdentifier<Config>,
        builder: ConfigBuilder,
        ctx: ReadContext
    ) {
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name

        val data = access.read(BgpProtocolReader.UNDERLAY_BGP, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()

        parse(data, protKey, builder, vrfName)
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as GlobalBuilder).config = readValue
    }

    companion object {

        fun parse(data: Bgp?, protKey: ProtocolKey, builder: ConfigBuilder, vrfName: String) {
            data?.let {
                it.instance.orEmpty()
                        .find { it.instanceName.value == protKey.name }
                        ?.let { builder.fromUnderlay(it, vrfName) }
            }
        }
    }
}

@VisibleForTesting
fun ConfigBuilder.fromUnderlay(underlayInstance: Instance, vrfName: String) {
    // each instance can only have one AS despite there is a list in cisco yang
    val firstAs = underlayInstance.instanceAs.orEmpty().firstOrNull()?.`as`

    BgpProtocolReader.getFirst4ByteAs(underlayInstance)
            ?.let { fourByteAs ->

                // Set router ID for appropriate VRF
                if (NetworInstance.DEFAULT_NETWORK_NAME == vrfName) {
                    `as` = asFromDotNotation(firstAs?.value, fourByteAs.`as`.value)
                    fourByteAs.defaultVrf?.global?.routerId?.value?.let { routerId = DottedQuad(it) }
                } else {
                    fourByteAs.vrfs?.vrf.orEmpty()
                            .find { it.vrfName.value == vrfName }
                            ?.let {
                                `as` = asFromDotNotation(firstAs?.value, fourByteAs.`as`.value)
                                it.vrfGlobal?.routerId?.value?.let { routerId = DottedQuad(it) } }
                }
            }
}