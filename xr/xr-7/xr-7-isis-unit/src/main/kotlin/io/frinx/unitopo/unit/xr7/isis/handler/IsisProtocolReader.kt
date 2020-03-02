/*
 * Copyright © 2020 Frinx and others.
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
package io.frinx.unitopo.unit.xr7.isis.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.builder.Check
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.Isis
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.Instances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.ISIS
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class IsisProtocolReader(private val access: UnderlayAccess) :
        CompositeListReader.Child<Protocol, ProtocolKey, ProtocolBuilder> {

    override fun getCheck(): Check {
        return ChecksMap.PathCheck.Protocol.ISIS
    }

    override fun getAllIds(id: IID<Protocol>, context: ReadContext): List<ProtocolKey> {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        if (vrfName != NetworInstance.DEFAULT_NETWORK_NAME) {
            return emptyList()
        }

        return access.read(UNDERLAY_ISIS)
                .checkedGet()
                .orNull()
            ?.let {
                it.instance.orEmpty()
                    .map { ProtocolKey(ISIS::class.java, it.instanceName.value) }
            }.orEmpty()
    }

    override fun readCurrentAttributes(
        id: IID<Protocol>,
        builder: ProtocolBuilder,
        ctx: ReadContext
    ) {
        val key = id.firstKeyOf(Protocol::class.java)
        builder.name = key.name
        builder.identifier = key.identifier
    }

    companion object {
        val UNDERLAY_ISIS = IID.create(Isis::class.java).child(Instances::class.java)!!
    }
}