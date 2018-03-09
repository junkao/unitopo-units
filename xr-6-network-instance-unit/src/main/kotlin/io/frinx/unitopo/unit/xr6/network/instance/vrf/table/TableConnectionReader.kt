/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.vrf.table

import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.cli.registry.common.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.handler.table.BgpTableConnectionReader
import io.frinx.unitopo.unit.xr6.ospf.handler.table.OspfTableConnectionReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.TableConnectionsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.TableConnection
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.TableConnectionBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.table.connections.TableConnectionKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class TableConnectionReader(access: UnderlayAccess) :
        ConfigListReaderCustomizer<TableConnection, TableConnectionKey, TableConnectionBuilder>,
        CompositeListReader<TableConnection, TableConnectionKey, TableConnectionBuilder>(
                listOf(BgpTableConnectionReader(access), OspfTableConnectionReader(access))) {

    override fun merge(builder: Builder<out DataObject>, list: List<TableConnection>) {
        (builder as TableConnectionsBuilder).tableConnection = list
    }

    override fun getBuilder(id: InstanceIdentifier<TableConnection>) = TableConnectionBuilder()
}
