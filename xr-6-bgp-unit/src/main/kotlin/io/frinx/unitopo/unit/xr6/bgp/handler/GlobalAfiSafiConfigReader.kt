package io.frinx.unitopo.unit.xr6.bgp.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.network.instance.protocol.bgp.common.BgpReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.afi.safi.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.afi.safi.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class GlobalAfiSafiConfigReader(private val access: UnderlayAccess) : BgpReader.BgpConfigReader<Config, ConfigBuilder> {

    override fun merge(parentBuilder: Builder<out DataObject>, config: Config) {
        (parentBuilder as AfiSafiBuilder).config = config
    }

    override fun getBuilder(p0: InstanceIdentifier<Config>) = ConfigBuilder()

    override fun readCurrentAttributesForType(instanceIdentifier: InstanceIdentifier<Config>, builder: ConfigBuilder, readContext: ReadContext) {
        val afiSafiKey = instanceIdentifier.firstKeyOf(AfiSafi::class.java)
        builder.afiSafiName = afiSafiKey.afiSafiName
    }
}