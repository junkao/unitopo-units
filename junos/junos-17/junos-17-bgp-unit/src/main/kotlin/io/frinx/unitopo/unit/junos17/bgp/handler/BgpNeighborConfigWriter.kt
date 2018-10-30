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
package io.frinx.unitopo.unit.junos17.bgp.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpProtocolReader.Companion.UNDERLAY_PROTOCOL_BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.PeerType
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Ipaddr
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.Group as JunosGroup
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.Group.Type as JunosGroupType
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.GroupBuilder as JunosGroupBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.group.Neighbor as JunosNeighbor
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.group.NeighborBuilder as JunosNeighborBuilder

class BgpNeighborConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val neighbor = getUnderlayNeigbor(dataAfter)
        val group = getunderlayGroup(dataAfter)
        val isUnderlayGroupPresent =
                underlayAccess.read(UNDERLAY_PROTOCOL_BGP.child(JunosGroup::class.java, group.key))
                        .checkedGet().isPresent

        if (isUnderlayGroupPresent) {
            try {
                underlayAccess.put(UNDERLAY_PROTOCOL_BGP
                        .child(JunosGroup::class.java, group.key)
                        .child(JunosNeighbor::class.java, neighbor.key), neighbor)
            } catch (e: Exception) {
                throw WriteFailedException(id, e)
            }
        } else {
            throw WriteFailedException(id, String.format("Write failed: Underlay Group: %s is missing", group.name))
        }
    }

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val neighborBefore = getUnderlayNeigbor(dataBefore)
        val groupBefore = getunderlayGroup(dataBefore)
        try {
            underlayAccess.delete(UNDERLAY_PROTOCOL_BGP
                    .child(JunosGroup::class.java, groupBefore.key)
                    .child(JunosNeighbor::class.java, neighborBefore.key))
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val neighborAfter = getUnderlayNeigbor(dataAfter)
        val groupAfter = getunderlayGroup(dataAfter)

        val neighborBefore = getUnderlayNeigbor(dataBefore)
        val groupBefore = getunderlayGroup(dataBefore)

        val isUnderlayGroupAfterPresent =
                underlayAccess.read(UNDERLAY_PROTOCOL_BGP.child(JunosGroup::class.java, groupAfter.key))
                        .checkedGet().isPresent

        if (groupBefore.name != groupAfter.name) {
            // if peer group is changed, we need to move neighbor to new group
            if (isUnderlayGroupAfterPresent) {
                try {
                    underlayAccess.delete(UNDERLAY_PROTOCOL_BGP
                            .child(JunosGroup::class.java, groupBefore.key)
                            .child(JunosNeighbor::class.java, neighborBefore.key))

                    underlayAccess.put(UNDERLAY_PROTOCOL_BGP
                            .child(JunosGroup::class.java, groupAfter.key)
                            .child(JunosNeighbor::class.java, neighborAfter.key), neighborAfter)
                } catch (e: Exception) {
                    throw WriteFailedException(id, e)
                }
            } else {
                throw WriteFailedException(id,
                    String.format("Update failed: Underlay Group: %s is missing", groupAfter.name))
            }
        } else {
            if (isUnderlayGroupAfterPresent) {
                try {
                    underlayAccess.merge(UNDERLAY_PROTOCOL_BGP
                            .child(JunosGroup::class.java, groupAfter.key)
                            .child(JunosNeighbor::class.java, neighborAfter.key), neighborAfter)
                } catch (e: Exception) {
                    throw WriteFailedException(id, e)
                }
            } else {
                throw WriteFailedException(id,
                    String.format("Update failed: Underlay Group: %s is missing", groupAfter.name))
            }
        }
    }

    private fun getUnderlayNeigbor(dataAfter: Config): JunosNeighbor {
        val neighborBuilder = JunosNeighborBuilder()
        neighborBuilder.name = Ipaddr(dataAfter.neighborAddress?.ipv4Address?.value)
        neighborBuilder.peerAs = dataAfter.peerAs?.value.toString()
        return neighborBuilder.build()
    }
    private fun getunderlayGroup(dataAfter: Config): JunosGroup {
        val builder = JunosGroupBuilder()
        builder.name = dataAfter.peerGroup
        builder.type = parseJunosGroupType(dataAfter.peerType)
        return builder.build()
    }

    private fun parseJunosGroupType(peerType: PeerType?): JunosGroupType? {
        return when (peerType) {
            PeerType.INTERNAL -> JunosGroupType.Internal
            PeerType.EXTERNAL -> JunosGroupType.External
            else -> null
        }
    }
}