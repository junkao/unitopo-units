/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.Ospf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.Processes
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev160512.OSPF
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedException
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfProtocolReader(private val access: UnderlayAccess) : ListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder> {

    @Throws(ReadFailedException::class)
    override fun getAllIds(id: IID<Protocol>, context: ReadContext): List<ProtocolKey> {
        try {
            return access.read(UNDERLAY_OSPF)
                    .checkedGet()
                    .orNull()
                    // FIXME filter only per VRF
                    ?.let { it.process.orEmpty().map { ProtocolKey(TYPE, it.processName?.value) }.toList() }
                    .orEmpty()
        } catch (e: MdSalReadFailedException) {
            throw ReadFailedException(id, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, readData: List<Protocol>) {
        // NOOP
    }

    override fun getBuilder(id: IID<Protocol>) = ProtocolBuilder()

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(id: IID<Protocol>, builder: ProtocolBuilder, ctx: ReadContext) {
        val key = id.firstKeyOf(Protocol::class.java)
        if (key.identifier == TYPE) {
            builder.name = key.name
            builder.identifier = key.identifier
        }
    }

    companion object {
        private val UNDERLAY_OSPF = IID.create(Ospf::class.java).child(Processes::class.java)
        val TYPE: Class<OSPF> = OSPF::class.java
    }
}