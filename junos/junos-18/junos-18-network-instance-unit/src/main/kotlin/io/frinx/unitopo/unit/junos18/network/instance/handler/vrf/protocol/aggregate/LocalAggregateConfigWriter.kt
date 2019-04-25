/*
 * Copyright © 2018 Frinx and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.frinx.unitopo.unit.junos18.network.instance.handler.vrf.protocol.aggregate

import io.frinx.translate.unit.commons.handler.spi.CompositeWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.bgp.handler.aggregate.BgpAggregateConfigWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.Config

class LocalAggregateConfigWriter(access: UnderlayAccess) : CompositeWriter<Config>(
    listOf(
        BgpAggregateConfigWriter(access)
    )
)