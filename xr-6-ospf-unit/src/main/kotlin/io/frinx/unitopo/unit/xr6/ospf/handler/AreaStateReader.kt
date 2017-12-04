package io.frinx.unitopo.unit.xr6.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.unitopo.unit.xr6.ospf.common.OspfReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.structure.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.structure.StateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.AreaBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.AreaKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class AreaStateReader : OspfReader.OspfOperReader<State, StateBuilder> {

    // FIXME Duplicate code with config

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<State>) = StateBuilder()

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributesForType(instanceIdentifier: InstanceIdentifier<State>, configBuilder: StateBuilder, readContext: ReadContext) {
        configBuilder.identifier = instanceIdentifier.firstKeyOf<Area, AreaKey>(Area::class.java).identifier
    }

    override fun merge(builder: Builder<out DataObject>, config: State) {
        (builder as AreaBuilder).state = config
    }
}
