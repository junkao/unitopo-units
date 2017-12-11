package io.frinx.unitopo.unit.xr6.network.instance

import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.cli.registry.common.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.network.instance.l2p2p.cp.L2P2PConnectionPointsReader
import io.frinx.unitopo.unit.xr6.network.instance.l2vsi.cp.L2VSIConnectionPointsReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPoints
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPointsBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.*

class ConnectionPointsReader(cli: UnderlayAccess) : ConfigReaderCustomizer<ConnectionPoints, ConnectionPointsBuilder>,
        CompositeReader<ConnectionPoints, ConnectionPointsBuilder>(object : ArrayList<ReaderCustomizer<ConnectionPoints, ConnectionPointsBuilder>>() {
    init {
        add(L2P2PConnectionPointsReader(cli))
        add(L2VSIConnectionPointsReader(cli))
    }
}), ReaderCustomizer<ConnectionPoints, ConnectionPointsBuilder> {

    override fun getBuilder(id: InstanceIdentifier<ConnectionPoints>): ConnectionPointsBuilder {
        return ConnectionPointsBuilder()
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: ConnectionPoints) {
        (parentBuilder as NetworkInstanceBuilder).connectionPoints = readValue
    }
}
