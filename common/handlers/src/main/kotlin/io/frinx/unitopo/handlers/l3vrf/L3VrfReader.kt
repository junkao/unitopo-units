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
package io.frinx.unitopo.handlers.l3vrf

import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.fd.honeycomb.translate.util.RWUtils
import io.frinx.translate.unit.commons.handler.spi.TypedReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.DEFAULTINSTANCE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L3VRF
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.AbstractMap
import java.util.function.Function

interface L3VrfReader<O : DataObject, B : Builder<O>> : TypedReader<O, B> {

    override fun getParentCheck(id: InstanceIdentifier<O>?):
        AbstractMap.SimpleEntry<InstanceIdentifier<out DataObject>, Function<DataObject, Boolean>> {
        return AbstractMap.SimpleEntry(RWUtils.cutId(id!!, NetworkInstance::class.java)
            .child(Config::class.java), L3VRF_CHECK)
    }

    companion object {
        val L3VRF_CHECK = Function { config: DataObject ->
            (config as Config).type == L3VRF::class.java || config.type == DEFAULTINSTANCE::class.java
        }
    }

    interface L3VrfConfigReader<O : DataObject, B : Builder<O>> : L3VrfReader<O, B>, ConfigReaderCustomizer<O, B>
    interface L3VrfOperReader<O : DataObject, B : Builder<O>> : L3VrfReader<O, B>, OperReaderCustomizer<O, B>
}