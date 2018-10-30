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

package io.frinx.unitopo.unit.junos17.platform.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.OperListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.OsComponent
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.ComponentsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.components.Component
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.components.ComponentBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.components.ComponentKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.Collections

class ComponentReader(private val access: UnderlayAccess) : OperListReaderCustomizer<Component,
    ComponentKey,
    ComponentBuilder> {

    override fun getBuilder(p0: InstanceIdentifier<Component>): ComponentBuilder {
        return ComponentBuilder()
    }

    override fun merge(builder: Builder<out DataObject>, config: List<Component>) {
        (builder as ComponentsBuilder).component = config
    }

    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Component>,
        componentBuilder: ComponentBuilder,
        readContext: ReadContext
    ) {
        componentBuilder.name = instanceIdentifier.firstKeyOf(Component::class.java).name
    }

    @Throws(ReadFailedException::class)
    override fun getAllIds(id: InstanceIdentifier<Component>, context: ReadContext): List<ComponentKey> {
        return parseComponents()
    }

    private fun parseComponents(): List<ComponentKey> {
        return Collections.singletonList(OsComponent.OS_KEY)
    }
}