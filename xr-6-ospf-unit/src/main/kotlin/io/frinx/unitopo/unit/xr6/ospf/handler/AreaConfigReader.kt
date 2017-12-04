package io.frinx.unitopo.unit.xr6.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.unit.xr6.ospf.common.OspfReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.structure.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.structure.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.AreaBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.AreaKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class AreaConfigReader : OspfReader.OspfConfigReader<Config, ConfigBuilder> {

    override fun readCurrentAttributesForType(instanceIdentifier: InstanceIdentifier<Config>, configBuilder: ConfigBuilder, readContext: ReadContext) {
        configBuilder.identifier = instanceIdentifier.firstKeyOf<Area, AreaKey>(Area::class.java).identifier
    }

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<Config>) = ConfigBuilder()

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as AreaBuilder).config = config
    }
}
