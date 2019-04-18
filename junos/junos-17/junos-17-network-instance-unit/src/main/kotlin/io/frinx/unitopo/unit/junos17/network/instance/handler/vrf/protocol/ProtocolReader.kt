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
package io.frinx.unitopo.unit.junos17.network.instance.handler.vrf.protocol

import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.ospf.handler.OspfProtocolReader
import io.frinx.unitopo.unit.junos17.bgp.handler.BgpProtocolReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey

class ProtocolReader(underlayAccess: UnderlayAccess) : CompositeListReader<Protocol, ProtocolKey, ProtocolBuilder>(
        listOf(
            OspfProtocolReader(underlayAccess),
            BgpProtocolReader(underlayAccess)
        )
), ConfigListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder>