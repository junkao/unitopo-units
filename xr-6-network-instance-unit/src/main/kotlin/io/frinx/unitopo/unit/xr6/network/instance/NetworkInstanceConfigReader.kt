/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance

import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.cli.registry.common.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.network.instance.common.def.DefaultConfigReader
import io.frinx.unitopo.unit.xr6.network.instance.l2p2p.L2P2PConfigReader
import io.frinx.unitopo.unit.xr6.network.instance.l2vsi.L2VSIConfigReader
import io.frinx.unitopo.unit.xr6.network.instance.vrf.VrfConfigReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.*

class NetworkInstanceConfigReader(cli: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder>, CompositeReader<Config, ConfigBuilder>
(object : ArrayList<ReaderCustomizer<Config, ConfigBuilder>>() {
    init {
        add(VrfConfigReader(cli))
        add(DefaultConfigReader())
        add(L2P2PConfigReader(cli))
        add(L2VSIConfigReader(cli))
    }
}), ReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<Config>): ConfigBuilder {
        return ConfigBuilder()
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as NetworkInstanceBuilder).config = config
    }
}
