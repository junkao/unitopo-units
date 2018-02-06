/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.vrf.protocol

import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.cli.registry.common.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.handler.aggregates.BgpLocalAggregateReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.LocalAggregatesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.Aggregate
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class LocalAggregateReader(access: UnderlayAccess) : ConfigListReaderCustomizer<Aggregate, AggregateKey, AggregateBuilder>,
        CompositeListReader<Aggregate, AggregateKey, AggregateBuilder>(listOf(
                BgpLocalAggregateReader(access)
        )) {

    override fun getBuilder(p0: InstanceIdentifier<Aggregate>) = AggregateBuilder()

    override fun merge(builder: Builder<out DataObject>, list: List<Aggregate>) {
        (builder as LocalAggregatesBuilder).aggregate = list
    }
}
