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

package io.frinx.unitopo.unit.junos.bfd.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader.Companion.IFCS
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.rev171117.bfd.top.bfd.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.rev171117.bfd.top.bfd.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.rev171117.bfd.top.bfd.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.rev171117.bfd.top.bfd.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Interfaces
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.Objects

class InterfaceReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    override fun readCurrentAttributes(id: InstanceIdentifier<Interface>, builder: InterfaceBuilder, p2: ReadContext) {
        val ifcId = id.firstKeyOf(Interface::class.java).id
        builder.key = InterfaceKey(ifcId)
    }

    override fun getAllIds(id: InstanceIdentifier<Interface>, context: ReadContext): List<InterfaceKey> {
        return getInterfaceIds(underlayAccess)
    }

    override fun merge(builder: Builder<out DataObject>, iface: MutableList<Interface>) {
        (builder as InterfacesBuilder).`interface` = iface
    }

    override fun getBuilder(id: InstanceIdentifier<Interface>): InterfaceBuilder = InterfaceBuilder()

    companion object {

        private fun getInterfaceIds(underlayAccess: UnderlayAccess): List<InterfaceKey> {
            return underlayAccess.read(IFCS, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()
                ?.let { parseInterfaceIds(it) }.orEmpty()
        }

        private fun parseInterfaceIds(it: Interfaces): List<InterfaceKey> {
            return it.`interface`.orEmpty()
                .filter { p -> Objects.nonNull(p.aggregatedEtherOptions?.bfdLivenessDetection) }
                .map { it.key }
                .map { InterfaceKey(it.name) }
        }
    }
}