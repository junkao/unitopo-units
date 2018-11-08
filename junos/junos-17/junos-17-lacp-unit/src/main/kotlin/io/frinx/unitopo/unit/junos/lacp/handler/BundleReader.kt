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

package io.frinx.unitopo.unit.junos.lacp.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader.Companion.IFCS
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.lacp.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.lacp.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.lacp.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.lacp.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Interfaces
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class BundleReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    override fun readCurrentAttributes(
        iid: InstanceIdentifier<Interface>,
        builder: InterfaceBuilder,
        context: ReadContext
    ) {
        val ifcId = iid.firstKeyOf(Interface::class.java).name
        builder.key = InterfaceKey(ifcId)
    }

    override fun getAllIds(iid: InstanceIdentifier<Interface>, context: ReadContext): List<InterfaceKey> {
        return underlayAccess.read(IFCS, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
            ?.let { parseInterfaceIds(it) }.orEmpty()
    }

    override fun merge(builder: Builder<out DataObject>, interfaces: MutableList<Interface>) {
        (builder as InterfacesBuilder).`interface` = interfaces
    }

    override fun getBuilder(iid: InstanceIdentifier<Interface>): InterfaceBuilder = InterfaceBuilder()

    companion object {
        private fun parseInterfaceIds(it: Interfaces): List<InterfaceKey> {
            return it.`interface`.orEmpty()
                .filter { port -> InterfaceConfigReader.parseIfcType(port.name) == Ieee8023adLag::class.java }
                .map { it.key }
                .map { InterfaceKey(it.name) }
        }
    }
}