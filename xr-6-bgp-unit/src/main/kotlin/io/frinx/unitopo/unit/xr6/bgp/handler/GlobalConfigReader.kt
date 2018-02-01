/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.network.instance.protocol.bgp.common.BgpReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.yang.rev170403.DottedQuad
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedException

class GlobalConfigReader(private val access: UnderlayAccess) : BgpReader.BgpConfigReader<Config, ConfigBuilder> {

    override fun getBuilder(id: InstanceIdentifier<Config>) = ConfigBuilder()

    override fun readCurrentAttributesForType(id: InstanceIdentifier<Config>, builder: ConfigBuilder, ctx: ReadContext) {
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        try {
            access.read(BgpProtocolReader.UNDERLAY_BGP)
                    .checkedGet()
                    .orNull()
                    ?.let {
                        it.instance.orEmpty()
                                .find { it.instanceName.value == protKey.name }
                                ?.let { builder.fromUnderlay(it, vrfName) }
                    }
        } catch (e: MdSalReadFailedException) {
            throw ReadFailedException(id, e)
        }
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as GlobalBuilder).config = readValue
    }
}

@VisibleForTesting
fun ConfigBuilder.fromUnderlay(underlayInstance: Instance, vrfName: String) {
    // each instance can only have one AS despite there is a list in cisco yang
    underlayInstance.instanceAs.orEmpty().firstOrNull()
            ?.fourByteAs.orEmpty().firstOrNull()
            ?.let {
                `as` = AsNumber(it.`as`.value)

                // Set router ID for appropriate VRF
                if (NetworInstance.DEFAULT_NETWORK_NAME == vrfName) {
                    it.defaultVrf?.global?.routerId?.value?.let { routerId = DottedQuad(it) }
                } else {
                    it.vrfs?.vrf.orEmpty()
                            .find { it.vrfName.value == vrfName }
                            ?.let { it.vrfGlobal?.routerId?.value?.let { routerId = DottedQuad(it) } }
                }
            }
}
