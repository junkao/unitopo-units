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
package io.frinx.unitopo.unit.xr66.bgp.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.builder.Check
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr66.bgp.IID
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.Bgp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP

open class BgpProtocolReader(private val access: UnderlayAccess) :
    CompositeListReader.Child<Protocol, ProtocolKey, ProtocolBuilder> {

    override fun getCheck(): Check {
        return ChecksMap.PathCheck.Protocol.BGP
    }

    override fun getBuilder(p0: IID<Protocol>) = ProtocolBuilder()

    override fun getAllIds(id: IID<Protocol>, context: ReadContext): List<ProtocolKey> {
        val data = access.read(BgpProtocolReader.UNDERLAY_BGP, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        return parseIds(data, vrfName)
    }

    override fun readCurrentAttributes(id: IID<Protocol>, builder: ProtocolBuilder, ctx: ReadContext) {
        val key = id.firstKeyOf(Protocol::class.java)
        builder.name = key.name
        builder.identifier = key.identifier
    }

    companion object {
        val UNDERLAY_BGP = IID.create(Bgp::class.java)!!

        fun getFirst4ByteAs(underlayInstance: Instance?): FourByteAs? =
            underlayInstance?.instanceAs.orEmpty().firstOrNull()
                ?.fourByteAs.orEmpty().firstOrNull()

        fun parseIds(data: Bgp?, vrfName: String): List<ProtocolKey> {
            return when (vrfName) {
                "default" -> data?.let {
                    it.instance.orEmpty().map {
                        ProtocolKey(BGP::class.java, it.instanceName.value)
                    }
                }.orEmpty()
                else -> data?.let {
                    it.instance.orEmpty().filter {
                        val fbAs = getFirst4ByteAs(it)
                        fbAs?.vrfs?.vrf.orEmpty().filter {
                            it.vrfName.value == vrfName
                        }.orEmpty().isEmpty() == false
                    }.map {
                        ProtocolKey(BGP::class.java, it.instanceName.value)
                    }
                }.orEmpty()
            }
        }
    }
}