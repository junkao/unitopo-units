/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.vrf

import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.cli.registry.common.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import io.frinx.unitopo.unit.xr6.lr.handler.StaticProtocolReader
import io.frinx.unitopo.unit.xr6.ospf.handler.OspfProtocolReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ProtocolsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class ProtocolReader(underlayAccess: UnderlayAccess) :
        CompositeListReader<Protocol, ProtocolKey, ProtocolBuilder>(getChildren(underlayAccess)) {

    override fun merge(builder: Builder<out DataObject>, list: List<Protocol>) {
        (builder as ProtocolsBuilder).`protocol` = list
    }

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<Protocol>): ProtocolBuilder {
        return ProtocolBuilder()
    }
}

private fun getChildren(underlayAccess: UnderlayAccess): List<ListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder>> =
        listOf(BgpProtocolReader(underlayAccess), OspfProtocolReader(underlayAccess), StaticProtocolReader())
