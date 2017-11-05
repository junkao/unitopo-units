package io.frinx.unitopo.unit.xr6.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.ospf.common.OspfReader
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.State
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.StateBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class GlobalStateReader(private val access: UnderlayAccess) : OspfReader.OspfOperReader<State, StateBuilder> {

    override fun getBuilder(id: InstanceIdentifier<State>) = StateBuilder()

    override fun readCurrentAttributesForType(id: InstanceIdentifier<State>, builder: StateBuilder, ctx: ReadContext) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java)
        val protKey = id.firstKeyOf(Protocol::class.java)

        try {
            GlobalConfigReader.readProcess(access, protKey, { builder.fromUnderlay(it, vrfName.name) })
        } catch (e: ReadFailedException) {
            throw io.fd.honeycomb.translate.read.ReadFailedException(id, e)
        }
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: State) {
        (parentBuilder as GlobalBuilder).state = readValue
    }
}

private fun StateBuilder.fromUnderlay(p: Process, vrfName: String) {
    GlobalConfigReader.getRouterId(vrfName, p)?.let {
        routerId = it
    }
}