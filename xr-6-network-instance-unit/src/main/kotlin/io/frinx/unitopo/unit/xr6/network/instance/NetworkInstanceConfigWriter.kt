package io.frinx.unitopo.unit.xr6.network.instance

import com.google.common.collect.Lists
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.frinx.cli.registry.common.CompositeWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.network.instance.common.def.DefaultConfigWriter
import io.frinx.unitopo.unit.xr6.network.instance.l2p2p.L2P2PConfigWriter
import io.frinx.unitopo.unit.xr6.network.instance.vrf.VrfConfigWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config

class NetworkInstanceConfigWriter(cli: UnderlayAccess) : CompositeWriter<Config>(Lists.newArrayList<WriterCustomizer<Config>>(
        VrfConfigWriter(cli),
        DefaultConfigWriter(),
        L2P2PConfigWriter(cli)))
