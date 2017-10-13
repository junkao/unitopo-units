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
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.Ospf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.Processes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.Global
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.StateBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.Ospfv2Builder
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedException
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class Ospfv2GlobalReader(private val access: UnderlayAccess) : ReaderCustomizer<Global, GlobalBuilder> {

    override fun getBuilder(id: IID<Global>) = GlobalBuilder()

    override fun readCurrentAttributes(id: IID<Global>, builder: GlobalBuilder, ctx: ReadContext) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java)
        val protKey = id.firstKeyOf(Protocol::class.java)
        if (protKey.identifier != OspfProtocolReader.TYPE) {
            return
        }

        try {
            readProcess(access, protKey, { builder.fromUnderlay(it, vrfName.name) })
        } catch (e: MdSalReadFailedException) {
            throw ReadFailedException(id, e)
        }
    }


    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Global) {
        (parentBuilder as Ospfv2Builder).global = readValue
    }

    companion object {
        private val UNDERLAY_OSPF_PROCESSES = IID.create(Ospf::class.java).child(Processes::class.java)

        fun readProcess(access: UnderlayAccess, protKey: ProtocolKey, handler: (Process) -> Unit) {
            getProcess(access, protKey)
                    ?.let(handler)
        }

        fun getProcess(access: UnderlayAccess, protKey: ProtocolKey): Process? {
            return access.read(UNDERLAY_OSPF_PROCESSES)
                    .checkedGet()
                    .orNull()
                    ?.process.orEmpty()
                    .find { it.processName.value == protKey.name }
        }
    }
}

private fun GlobalBuilder.fromUnderlay(p: Process, vrfName: String) {
    // Set router ID for appropriate VRF
    var routerId: DottedQuad? = null
    if (NetworInstance.DEFAULT_NETWORK_NAME == vrfName) {
        p.defaultVrf?.routerId?.value?.let { routerId = DottedQuad(it) }
    } else {
        p.vrfs?.vrf.orEmpty()
                .find { it.vrfName.value == vrfName }
                ?.let { routerId = DottedQuad(it.routerId.value) }
    }

    // Set child readers
    // TODO split into dedicated readers
    if (routerId != null) {
        config = ConfigBuilder().setRouterId(DottedQuad(routerId)).build()
        state = StateBuilder().setRouterId(DottedQuad(routerId)).build()
    }
}
