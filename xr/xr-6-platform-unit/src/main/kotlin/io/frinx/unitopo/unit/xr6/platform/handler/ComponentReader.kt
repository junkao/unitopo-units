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
import io.fd.honeycomb.translate.spi.read.OperListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.sc.invmgr.admin.oper.rev151109.Inventory
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.sc.invmgr.admin.oper.rev151109.inventory.Racks
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.ComponentsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.components.Component
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.components.ComponentBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.components.ComponentKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.regex.Pattern

class ComponentReader(private val access: UnderlayAccess)
    : OperListReaderCustomizer<Component, ComponentKey, ComponentBuilder> {

    override fun getAllIds(
        instanceIdentifier: InstanceIdentifier<Component>,
        readContext: ReadContext
    ): List<ComponentKey> {

        return access.read(RACKS_ID).checkedGet().orNull()
                // TODO we should be able to read all nodes from XR inventory
                // (all the Racks, Slots, Cards, Fans etc.) not just line cards
                ?.let { parseLineCardComponentIds(it) }
                .orEmpty()
    }

    override fun merge(builder: Builder<out DataObject>, list: List<Component>) {
        (builder as ComponentsBuilder).component = list
    }

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<Component>): ComponentBuilder {
        return ComponentBuilder()
    }

    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Component>,
        componentBuilder: ComponentBuilder,
        readContext: ReadContext
    ) {
        val componentName = instanceIdentifier.firstKeyOf(Component::class.java).name
        componentBuilder.name = componentName
    }

    companion object {

        private val RACKS_ID = InstanceIdentifier.create(Inventory::class.java).child(Racks::class.java)

        // TODO Make this regexp more robust, so we can match various line cards
        private val LINE_CARD_PATTERN = Pattern.compile(".*Line Card.*")

        // TODO better javadoc
        /**
         * Parses Racks container and looks for line cards in Cards containers
         * @param racks
         * @return
         */
        private fun parseLineCardComponentIds(racks: Racks): List<ComponentKey> {
            // This basically does goes to the inventory's cards level
            // and searches for line cards
            return racks.rack?.flatMap { it?.slots?.slot.orEmpty() }
                    ?.flatMap { it?.cards?.card.orEmpty() }
                    ?.map { it?.basicAttributes?.basicInfo }
                    ?.filter { it?.name != null && it.description != null }
                    ?.filter { LINE_CARD_PATTERN.matcher(it?.description).matches() }
                    // TODO we are using card name as a Component key, but
                    // do we have assurance that our keys will be unique?
                    ?.map { ComponentKey(it?.name) }.orEmpty()
        }
    }
}