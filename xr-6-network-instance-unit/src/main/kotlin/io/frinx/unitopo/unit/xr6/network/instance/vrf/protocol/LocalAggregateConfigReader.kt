/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.vrf.protocol

import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.cli.registry.common.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.handler.aggregates.BgpLocalAggregateConfigReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class LocalAggregateConfigReader(access: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder>,
        CompositeReader<Config, ConfigBuilder>(listOf(BgpLocalAggregateConfigReader())) {

    override fun getBuilder(p0: InstanceIdentifier<Config>) = ConfigBuilder()

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as AggregateBuilder).config = config
    }
}
