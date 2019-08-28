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
package io.frinx.unitopo.unit.junos17.bgp.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpProtocolReader.Companion.UNDERLAY_PROTOCOL_BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.base.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.PeerType
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.Group
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.Group.Type
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class BgpPeerGroupConfigReader(private val access: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Config>,
        builder: ConfigBuilder,
        ctx: ReadContext
    ) {
            val read = access.read(UNDERLAY_PROTOCOL_BGP.child(Group::class.java))
            read.checkedGet().orNull()
                ?.let {
                    builder.peerGroupName = it.name
                    builder.peerType = resolvePeerType(it.type) }
    }

    fun resolvePeerType(groupType: Type): PeerType {
        if (groupType == Type.Internal) {
            return PeerType.INTERNAL
        } else {
            return PeerType.EXTERNAL
        }
    }
}