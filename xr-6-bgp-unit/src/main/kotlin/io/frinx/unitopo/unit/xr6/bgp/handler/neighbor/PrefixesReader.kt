package io.frinx.unitopo.unit.xr6.bgp.handler.neighbor

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.network.instance.protocol.bgp.common.BgpReader
import io.frinx.unitopo.unit.xr6.bgp.UnderlayOperNeighbor
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.BgpAfi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.afi.safi.StateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.state.Prefixes
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.state.PrefixesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.*
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class PrefixesReader(private val access: UnderlayAccess) : BgpReader.BgpOperReader<Prefixes, PrefixesBuilder> {

    override fun merge(parentBuilder: Builder<out DataObject>, state: Prefixes) {
        (parentBuilder as StateBuilder).prefixes = state
    }

    override fun getBuilder(p0: InstanceIdentifier<Prefixes>) = PrefixesBuilder()

    override fun readCurrentAttributesForType(id: InstanceIdentifier<Prefixes>, builder: PrefixesBuilder, readContext: ReadContext) {
        val neighborKey = id.firstKeyOf(Neighbor::class.java)
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val afiSafiKey = id.firstKeyOf(AfiSafi::class.java)

        val data = access.read(NeighborStateReader.getId(protKey, vrfKey, neighborKey))
                .checkedGet()
                .orNull()

        builder.fromUnderlay(data, afiSafiKey)
    }
}

fun PrefixesBuilder.fromUnderlay(data: UnderlayOperNeighbor?, afiSafiKey: AfiSafiKey) {
    data?.afData.orEmpty()
            .find { it.afName.toOpenconfig() == afiSafiKey.afiSafiName }
            ?.let {
                received = it.prefixesAccepted
            }
}

fun BgpAfi.toOpenconfig(): Class<out AFISAFITYPE>? {
    when (this) {
        BgpAfi.Ipv4 -> return IPV4UNICAST::class.java
        BgpAfi.VpNv4 -> return L3VPNIPV4UNICAST::class.java
        BgpAfi.VpNv6 -> return L3VPNIPV6UNICAST::class.java
        BgpAfi.Ipv6 -> return IPV6UNICAST::class.java
    }

    return null
}