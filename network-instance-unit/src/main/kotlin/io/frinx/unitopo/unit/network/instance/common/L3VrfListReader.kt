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
package io.frinx.unitopo.unit.network.instance.common

import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.fd.honeycomb.translate.spi.read.OperListReaderCustomizer
import io.fd.honeycomb.translate.util.RWUtils
import io.frinx.translate.unit.commons.registry.common.TypedListReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.Identifiable
import org.opendaylight.yangtools.yang.binding.Identifier
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.AbstractMap
import java.util.function.Function

interface L3VrfListReader<O : DataObject, K : Identifier<O>, B : Builder<O>> : TypedListReader<O, K, B>
    where O : Identifiable<K> {

    override fun getParentCheck(id: InstanceIdentifier<O>?) =
            AbstractMap.SimpleEntry<InstanceIdentifier<out DataObject>, Function<DataObject, Boolean>>(
                    RWUtils.cutId(id!!, NetworkInstance::class.java).child(Config::class.java),
                    L3VrfReader.L3VRF_CHECK)

    interface L3VrfConfigListReader<O : DataObject, K : Identifier<O>, B : Builder<O>> : L3VrfListReader<O, K, B>,
        ConfigListReaderCustomizer<O, K, B> where O : Identifiable<K>
    interface L3VrfOperListReader<O : DataObject, K : Identifier<O>, B : Builder<O>> : L3VrfListReader<O, K, B>,
        OperListReaderCustomizer<O, K, B> where O : Identifiable<K>
}