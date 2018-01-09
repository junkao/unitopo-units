/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.common

import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.fd.honeycomb.translate.util.RWUtils
import io.frinx.cli.registry.common.TypedReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L2P2P
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.*
import java.util.function.Function

interface L2p2pReader<O : DataObject, B : Builder<O>> : TypedReader<O, B> {

    override fun getParentCheck(id: InstanceIdentifier<O>?): AbstractMap.SimpleEntry<InstanceIdentifier<out DataObject>, Function<DataObject, Boolean>> {
        return AbstractMap.SimpleEntry(RWUtils.cutId(id!!, NetworkInstance::class.java).child(Config::class.java), L2P2P_CHECK)
    }

    companion object {
        val L2P2P_CHECK = Function { config: DataObject -> (config as Config).type == L2P2P::class.java }
    }

    interface L2p2pConfigReader<O : DataObject, B : Builder<O>> : L2p2pReader<O, B>, ConfigReaderCustomizer<O, B>
    interface L2p2pOperReader<O : DataObject, B : Builder<O>> : L2p2pReader<O, B>, OperReaderCustomizer<O, B>
}