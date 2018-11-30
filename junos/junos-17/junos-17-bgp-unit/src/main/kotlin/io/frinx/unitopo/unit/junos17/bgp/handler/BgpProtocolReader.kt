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

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.handlers.bgp.BgpReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Configuration
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Protocols
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.RoutingOptions
import java.util.Collections
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedEx
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.Bgp as JunosBgp
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.routing.options.AutonomousSystem as JunosAutonomousSystem
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class BgpProtocolReader(private val access: UnderlayAccess) :
        BgpReader.BgpConfigReader<Protocol, ProtocolBuilder>,
        CompositeListReader.Child<Protocol, ProtocolKey, ProtocolBuilder> {

    override fun getBuilder(p0: IID<Protocol>): ProtocolBuilder {
        // NOOP
        throw UnsupportedOperationException("Should not be invoked")
    }

    override fun getAllIds(id: IID<Protocol>, context: ReadContext): List<ProtocolKey> {
        return try {
            when (access.read(UNDERLAY_PROTOCOL_BGP).checkedGet().isPresent) {
                true -> Collections.singletonList(ProtocolKey(BgpReader.TYPE, BgpReader.NAME))
                else -> emptyList()
            }
        } catch (e: MdSalReadFailedEx) {
            throw ReadFailedException(id, e)
        }
    }

    override fun readCurrentAttributesForType(id: IID<Protocol>, builder: ProtocolBuilder, ctx: ReadContext) {
        val key = id.firstKeyOf(Protocol::class.java)
        builder.name = key.name
        builder.identifier = key.identifier
    }

    companion object {
        val UNDERLAY = IID.create(Configuration::class.java)
        val UNDERLAY_PROTOCOL = UNDERLAY.child(Protocols::class.java)!!
        val UNDERLAY_PROTOCOL_BGP = UNDERLAY_PROTOCOL.child(JunosBgp::class.java)!!

        val UNDERLAY_RT_OPT = UNDERLAY.child(RoutingOptions::class.java)!!
        val UNDERLAY_RT_OPT_AS = UNDERLAY_RT_OPT.child(JunosAutonomousSystem::class.java)!!
    }
}