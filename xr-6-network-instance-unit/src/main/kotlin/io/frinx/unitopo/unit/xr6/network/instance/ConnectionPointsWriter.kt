package io.frinx.unitopo.unit.xr6.network.instance

import com.google.common.collect.Lists
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.frinx.cli.registry.common.CompositeWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.network.instance.l2p2p.cp.L2P2PConnectionPointsWriter
import io.frinx.unitopo.unit.xr6.network.instance.l2vsi.cp.L2VSIConnectionPointsWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConnectionPoints

class ConnectionPointsWriter(cli: UnderlayAccess) : CompositeWriter<ConnectionPoints>(Lists.newArrayList<WriterCustomizer<ConnectionPoints>>(
        L2P2PConnectionPointsWriter(cli),
        L2VSIConnectionPointsWriter(cli)))
