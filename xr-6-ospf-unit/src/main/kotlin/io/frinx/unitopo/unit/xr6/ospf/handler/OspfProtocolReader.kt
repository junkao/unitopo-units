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
package io.frinx.unitopo.unit.xr6.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.cli.registry.common.CompositeListReader
import io.frinx.unitopo.handlers.ospf.OspfReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.Ospf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.Processes
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedException
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfProtocolReader(private val access: UnderlayAccess) :
        OspfReader.OspfConfigReader<Protocol, ProtocolBuilder>,
        CompositeListReader.Child<Protocol, ProtocolKey, ProtocolBuilder> {

    override fun getBuilder(p0: org.opendaylight.yangtools.yang.binding.InstanceIdentifier<Protocol>): ProtocolBuilder {
        // NOOP
        throw UnsupportedOperationException("Should not be invoked")
    }

    @Throws(ReadFailedException::class)
    override fun getAllIds(id: IID<Protocol>, context: ReadContext): List<ProtocolKey> {
        return access.read(UNDERLAY_OSPF)
                .checkedGet()
                .orNull()
                // FIXME filter only per VRF
                ?.let { it.process.orEmpty().map { ProtocolKey(OspfReader.TYPE, it.processName?.value) }.toList() }
                .orEmpty()
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributesForType(id: IID<Protocol>, builder: ProtocolBuilder, ctx: ReadContext) {
        val key = id.firstKeyOf(Protocol::class.java)
        builder.name = key.name
        builder.identifier = key.identifier
    }

    companion object {
        val UNDERLAY_OSPF = IID.create(Ospf::class.java).child(Processes::class.java)
    }
}
