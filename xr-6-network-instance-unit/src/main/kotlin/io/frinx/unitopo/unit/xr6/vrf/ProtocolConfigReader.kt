/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.vrf

import org.opendaylight.yangtools.yang.binding.DataObject
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.Config
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.ConfigBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.concepts.Builder

class ProtocolConfigReader : ReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<Config>): ConfigBuilder {
        return ConfigBuilder()
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(instanceIdentifier: InstanceIdentifier<Config>, configBuilder: ConfigBuilder, readContext: ReadContext) {
        val protocolKey = instanceIdentifier.firstKeyOf(Protocol::class.java)
        configBuilder.`identifier` = protocolKey.identifier
        configBuilder.`name` = protocolKey.name
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as ProtocolBuilder).`config` = config
    }
}