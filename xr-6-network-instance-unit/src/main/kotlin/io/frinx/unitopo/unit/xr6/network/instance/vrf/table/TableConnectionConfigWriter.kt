/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.vrf.table

import io.frinx.cli.registry.common.CompositeWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.handler.table.BgpTableConnectionWriter
import io.frinx.unitopo.unit.xr6.ospf.handler.table.OspfTableConnectionWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.table.connection.Config

class TableConnectionConfigWriter(access: UnderlayAccess) : CompositeWriter<Config>(
        listOf(BgpTableConnectionWriter(access), OspfTableConnectionWriter(access)))
