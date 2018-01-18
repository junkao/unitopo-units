/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance

import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.network.instance.common.def.DefaultReader
import io.frinx.unitopo.unit.xr6.network.instance.l2p2p.L2P2PReader
import io.frinx.unitopo.unit.xr6.network.instance.l2vsi.L2VSIReader
import io.frinx.unitopo.unit.xr6.network.instance.vrf.VrfReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import io.frinx.unitopo.unit.network.instance.NetworkInstanceReader

class NetworkInstanceReader(access: UnderlayAccess) : NetworkInstanceReader(object : ArrayList<ListReaderCustomizer<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder>>() {
    init {
        add(VrfReader(access))
        add(DefaultReader())
        add(L2P2PReader(access))
        add(L2VSIReader(access))
    }
})
