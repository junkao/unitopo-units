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
package io.frinx.unitopo.unit.junos17.mpls.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.mpls.common.MplsReader
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnel.p2p_top.P2pTunnelAttributesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnel.p2p_top.p2p.tunnel.attributes.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnel.p2p_top.p2p.tunnel.attributes.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.tunnels.Tunnel
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.tunnels.TunnelKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv4Address
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.mpls.label.switched.path.label.switched.path.or.template.Case1
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class P2pAttributesConfigReader(private val access: UnderlayAccess) :
    MplsReader.MplsConfigReader<Config, ConfigBuilder> {

    override fun getBuilder(p0: InstanceIdentifier<Config>): ConfigBuilder = ConfigBuilder()

    override fun readCurrentAttributesForType(
        instanceIdentifier: InstanceIdentifier<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val name = instanceIdentifier.firstKeyOf<Tunnel, TunnelKey>(Tunnel::class.java).name
        try {
            TunnelConfigReader.readTunnel(access, name)?.let {
                configBuilder.destination = IpAddress(Ipv4Address((it.labelSwitchedPathOrTemplate as Case1).to.value))
            }
        } catch (e: ReadFailedException) {
            throw io.fd.honeycomb.translate.read.ReadFailedException(instanceIdentifier, e)
        }
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as P2pTunnelAttributesBuilder).config = readValue
    }
}