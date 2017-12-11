/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.common

import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.fd.honeycomb.translate.spi.read.OperListReaderCustomizer
import io.fd.honeycomb.translate.util.RWUtils
import io.frinx.cli.registry.common.TypedListReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.Identifiable
import org.opendaylight.yangtools.yang.binding.Identifier
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.*
import java.util.function.Function

interface L2p2pListReader<O : DataObject, K : Identifier<O>, B : Builder<O>> : TypedListReader<O, K, B> where O : Identifiable<K> {

    override fun getParentCheck(id: InstanceIdentifier<O>?) =
            AbstractMap.SimpleEntry<InstanceIdentifier<out DataObject>, Function<DataObject, Boolean>>(
                RWUtils.cutId(id!!, NetworkInstance::class.java).child(Config::class.java),
                L2p2pReader.L2P2P_CHECK)

    interface L2p2pConfigListReader<O : DataObject, K : Identifier<O>, B : Builder<O>> : L2p2pListReader<O, K, B>, ConfigListReaderCustomizer<O, K, B> where O : Identifiable<K>
    interface L2p2pOperListReader<O : DataObject, K : Identifier<O>, B : Builder<O>>:L2p2pListReader<O, K, B>, OperListReaderCustomizer<O, K, B> where O : Identifiable<K>


}
