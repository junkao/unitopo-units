package io.frinx.unitopo.unit.junos17.bgp.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpProtocolReader.Companion.UNDERLAY_PROTOCOL_BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.NeighborsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv4Address
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.group.Neighbor as JunosNeighbor

class BgpNeighborListReader(private val underlayAccess: UnderlayAccess) : ConfigListReaderCustomizer<Neighbor, NeighborKey, NeighborBuilder> {
    override fun readCurrentAttributes(iId: InstanceIdentifier<Neighbor>, builder: NeighborBuilder, ctx: ReadContext) {
        val neighborName = iId.firstKeyOf(Neighbor::class.java).neighborAddress.ipv4Address.value
        underlayAccess.read(UNDERLAY_PROTOCOL_BGP).checkedGet().orNull()?.let {
            it.group.orEmpty().forEach { group -> group?.neighbor.orEmpty()
                    .firstOrNull { neighbor -> neighbor.name?.value == neighborName }
                    ?.let { neighbor -> builder.fromUnderlay(neighbor) } }}
    }

    override fun getAllIds(id: InstanceIdentifier<Neighbor>, context: ReadContext): List<NeighborKey> {
        return underlayAccess.read(UNDERLAY_PROTOCOL_BGP).checkedGet().orNull()
                ?.let {
                    it.group.orEmpty().flatMap { group -> group?.neighbor.orEmpty() }
                            .map { NeighborKey(IpAddress(Ipv4Address(it.name.value))) }
                }.orEmpty()
    }

    override fun merge(builder: Builder<out DataObject>, neighbors: MutableList<Neighbor>) {
        (builder as NeighborsBuilder).neighbor = neighbors
    }

    override fun getBuilder(id: InstanceIdentifier<Neighbor>) = NeighborBuilder()

}

internal fun NeighborBuilder.fromUnderlay(neighbor: JunosNeighbor) {
    key = NeighborKey(IpAddress(Ipv4Address(neighbor.name?.value)))
}
