/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.network.instance

import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.cli.registry.common.CompositeListReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstancesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class NetworkInstanceReader(readers : ArrayList<ListReaderCustomizer<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder>>) : ConfigListReaderCustomizer<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder>,
        CompositeListReader<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder>(readers), ListReaderCustomizer<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder> {

    override fun merge(builder: Builder<out DataObject>, list: List<NetworkInstance>) {
        (builder as NetworkInstancesBuilder).networkInstance = list
    }

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<NetworkInstance>): NetworkInstanceBuilder {
        return NetworkInstanceBuilder()
    }
}
