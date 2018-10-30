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

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.mpls.common.MplsReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te._interface.attributes.Interface as OcInterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te._interface.attributes.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te._interface.attributes.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te._interface.attributes._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te._interface.attributes._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.rsvp.Interface
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class RsvpInterfaceConfigReader(private val underlayAccess: UnderlayAccess) :
    MplsReader.MplsConfigReader<Config, ConfigBuilder> {

    override fun getBuilder(p0: InstanceIdentifier<Config>): ConfigBuilder = ConfigBuilder()

    override fun readCurrentAttributesForType(
        instanceIdentifier: InstanceIdentifier<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val ifcName = RsvpInterfaceConfigWriter.formatIfcName(instanceIdentifier
            .firstKeyOf<OcInterface, InterfaceKey>(OcInterface::class.java).interfaceId.value)
        readInterface(underlayAccess, ifcName)?.let {
            configBuilder.interfaceId = InterfaceId(it.name.interfaceName.value)
        }
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as InterfaceBuilder).config = readValue
    }

    companion object {

        fun readInterface(underlayAccess: UnderlayAccess, name: String): Interface? {
            return underlayAccess.read(RsvpInterfaceReader.RSVP, LogicalDatastoreType.OPERATIONAL)
                    .checkedGet().orNull()
                    ?.`interface`.orEmpty().firstOrNull { it.name.interfaceName.value == name }
        }
    }
}