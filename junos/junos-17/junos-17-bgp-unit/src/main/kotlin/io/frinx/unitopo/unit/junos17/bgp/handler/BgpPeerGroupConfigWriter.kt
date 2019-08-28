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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpProtocolReader.Companion.UNDERLAY_PROTOCOL_BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.PeerType
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.Group.Type.External
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.Group.Type.Internal
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.Group as JunosGroup
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.GroupBuilder as JunosGroupBuilder

class BgpPeerGroupConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val group = getGroup(dataAfter)
            underlayAccess.put(UNDERLAY_PROTOCOL_BGP.child(JunosGroup::class.java, group.key), group)
    }

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val group = getGroup(dataBefore)
            underlayAccess.delete(UNDERLAY_PROTOCOL_BGP.child(JunosGroup::class.java, group.key))
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val groupAfter = getGroup(dataAfter)
            underlayAccess.merge(UNDERLAY_PROTOCOL_BGP.child(JunosGroup::class.java, groupAfter.key), groupAfter)
    }

    private fun getGroup(dataAfter: Config): JunosGroup {
        val builder = JunosGroupBuilder()
        builder.name = dataAfter.peerGroupName
        builder.type = if (dataAfter.peerType == PeerType.INTERNAL) { Internal } else { External }
        return builder.build()
    }
}