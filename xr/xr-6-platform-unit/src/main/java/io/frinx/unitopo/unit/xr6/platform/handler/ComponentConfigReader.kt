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
package io.frinx.unitopo.unit.xr6.platform.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.components.Component
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.components.ComponentBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.components.component.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.components.component.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class ComponentConfigReader : OperReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(id: InstanceIdentifier<Config>) = ConfigBuilder()

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Config>,
        builder: ConfigBuilder,
        ctx: ReadContext
    ) {
        builder.name = id.firstKeyOf(Component::class.java).name
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as ComponentBuilder).config = readValue
    }
}