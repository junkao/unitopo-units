/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.network.instance.common

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
import java.util.AbstractMap
import java.util.function.Function

interface L3VrfListReader<O : DataObject, K : Identifier<O>, B : Builder<O>> : TypedListReader<O, K, B> where O : Identifiable<K> {

    override fun getParentCheck(id: InstanceIdentifier<O>?)=
            AbstractMap.SimpleEntry<InstanceIdentifier<out DataObject>, Function<DataObject, Boolean>>(
                    RWUtils.cutId(id!!, NetworkInstance::class.java).child(Config::class.java),
                    L3VrfReader.L3VRF_CHECK)

    interface L3VrfConfigListReader<O : DataObject, K : Identifier<O>, B : Builder<O>> : L3VrfListReader<O, K, B>, ConfigListReaderCustomizer<O, K, B> where O : Identifiable<K>
    interface L3VrfOperListReader<O : DataObject, K : Identifier<O>, B : Builder<O>>:L3VrfListReader<O, K, B>, OperListReaderCustomizer<O, K, B> where O : Identifiable<K>

}
