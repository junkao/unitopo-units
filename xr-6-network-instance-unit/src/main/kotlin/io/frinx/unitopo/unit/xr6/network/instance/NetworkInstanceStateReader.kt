/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.cli.registry.common.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.network.instance.common.def.DefaultStateReader
import io.frinx.unitopo.unit.xr6.network.instance.l2p2p.L2P2PStateReader
import io.frinx.unitopo.unit.xr6.network.instance.l2vsi.L2VSIStateReader
import io.frinx.unitopo.unit.xr6.network.instance.vrf.VrfStateReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.StateBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.*

class NetworkInstanceStateReader(cli: UnderlayAccess) : CompositeReader<State, StateBuilder>(object : ArrayList<ReaderCustomizer<State, StateBuilder>>() {
    init {
        add(VrfStateReader(cli))
        add(DefaultStateReader())
        add(L2P2PStateReader(cli))
        add(L2VSIStateReader(cli))
    }
}), ReaderCustomizer<State, StateBuilder> {

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<State>): StateBuilder {
        return StateBuilder()
    }

    override fun merge(builder: Builder<out DataObject>, config: State) {
        (builder as NetworkInstanceBuilder).state = config
    }
}
