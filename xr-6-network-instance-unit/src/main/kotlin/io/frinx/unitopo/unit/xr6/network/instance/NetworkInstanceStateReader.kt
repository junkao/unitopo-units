/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.network.instance.NetworkInstanceStateReader
import io.frinx.unitopo.unit.network.instance.common.def.DefaultStateReader
import io.frinx.unitopo.unit.xr6.network.instance.l2p2p.L2P2PStateReader
import io.frinx.unitopo.unit.xr6.network.instance.l2vsi.L2VSIStateReader
import io.frinx.unitopo.unit.xr6.network.instance.vrf.VrfStateReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.StateBuilder

class NetworkInstanceStateReader(access: UnderlayAccess) : NetworkInstanceStateReader(object : ArrayList<ReaderCustomizer<State, StateBuilder>>() {
    init {
        add(VrfStateReader(access))
        add(DefaultStateReader())
        add(L2P2PStateReader(access))
        add(L2VSIStateReader(access))
    }
})
