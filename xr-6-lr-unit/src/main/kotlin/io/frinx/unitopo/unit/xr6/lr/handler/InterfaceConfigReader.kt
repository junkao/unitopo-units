/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lr.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222._interface.ref.InterfaceRefBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222._interface.ref._interface.ref.Config
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222._interface.ref._interface.ref.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHop
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceConfigReader(private val access: UnderlayAccess) : ReaderCustomizer<Config, ConfigBuilder> {

    override fun readCurrentAttributes(id: InstanceIdentifier<Config>, builder: ConfigBuilder, ctx: ReadContext) {
        val protKey = id.firstKeyOf(Protocol::class.java)
        if (protKey.identifier != StaticProtocolReader.TYPE) {
            return
        }
        val key = id.firstKeyOf(NextHop::class.java)

        val table = NextHopReader.parseNextHopTable(access, id.firstIdentifierOf(NextHop::class.java))

        val maybeContent2 = table?.vrfNextHopInterfaceName.orEmpty().stream().filter { f -> f.interfaceName.value == key.toString() }.findFirst()
        if (maybeContent2.isPresent) {
            builder.`interface` = maybeContent2.get().interfaceName.value
        } else {
            val maybeContent3 = table?.vrfNextHopInterfaceNameNextHopAddress.orEmpty().stream()
                    .filter { f -> NextHopReader.createComplexKey(f.interfaceName.value, f.nextHopAddress) == key }.findFirst()
            if (maybeContent3.isPresent) {
                builder.`interface` = maybeContent3.get().interfaceName.value
            }
        }
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as InterfaceRefBuilder).config = readValue
    }

    override fun getBuilder(p0: InstanceIdentifier<Config>): ConfigBuilder = ConfigBuilder()
}