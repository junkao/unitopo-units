/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.network.instance.protocol

import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.cli.registry.common.CompositeListReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ProtocolsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class ProtocolReaderComposite(readers : ArrayList<ListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder>>) :
        CompositeListReader<Protocol, ProtocolKey, ProtocolBuilder>(readers), ListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder> {

    override fun merge(builder: Builder<out DataObject>, list: List<Protocol>) {
        (builder as ProtocolsBuilder).protocol = list
    }

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<Protocol>): ProtocolBuilder {
        return ProtocolBuilder()
    }
}