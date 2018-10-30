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
package io.frinx.unitopo.unit.junos17.mpls.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.mpls.common.MplsListReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.mpls.top.mpls.TeInterfaceAttributesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te._interface.attributes.top.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te._interface.attributes.top.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te._interface.attributes.top.InterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Configuration
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Interfaces
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class TeInterfaceReader(private val underlayAccess: UnderlayAccess) :
    MplsListReader.MplsConfigListReader<Interface, InterfaceKey, InterfaceBuilder> {

    override fun getAllIdsForType(instanceIdentifier: InstanceIdentifier<Interface>, readContext: ReadContext):
        List<InterfaceKey> {
        try {
            return getInterfaceIds(underlayAccess)
        } catch (e: org.opendaylight.controller.md.sal.common.api.data.ReadFailedException) {
            throw ReadFailedException(instanceIdentifier, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, readData: List<Interface>) {
        (builder as TeInterfaceAttributesBuilder).`interface` = readData
    }

    override fun readCurrentAttributesForType(
        instanceIdentifier: InstanceIdentifier<Interface>,
        interfaceBuilder: InterfaceBuilder,
        readContext: ReadContext
    ) {
        val key = instanceIdentifier.firstKeyOf(Interface::class.java)
        interfaceBuilder.interfaceId = key.interfaceId
    }

    override fun getBuilder(p0: InstanceIdentifier<Interface>): InterfaceBuilder = InterfaceBuilder()

    companion object {

        val INTERFACES = InstanceIdentifier.create(Configuration::class.java).child(Interfaces::class.java)!!

        fun getInterfaceIds(underlayAccess: UnderlayAccess): List<InterfaceKey> {
            return underlayAccess.read(INTERFACES, LogicalDatastoreType.OPERATIONAL)
                .checkedGet()
                .orNull()
                ?.let { parseInterfaceIds(it) }.orEmpty()
        }

        @VisibleForTesting
        fun parseInterfaceIds(ifaces: Interfaces): List<InterfaceKey> {
            val keys = ArrayList<InterfaceKey>()
            for (iface in ifaces.`interface`.orEmpty()) {
                iface.unit.orEmpty().firstOrNull { it?.family?.mpls != null }?.let {
                    keys.add(InterfaceKey(InterfaceId(iface.name)))
                }
            }
            return keys
        }
    }
}