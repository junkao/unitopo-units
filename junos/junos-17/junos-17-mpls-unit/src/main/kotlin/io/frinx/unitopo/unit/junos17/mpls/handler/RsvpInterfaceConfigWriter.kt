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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te._interface.attributes._interface.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.rsvp.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.rsvp.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.rsvp.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te._interface.attributes.Interface as OcInterface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class RsvpInterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, data: Config, writeContext: WriteContext) {
        val (underlayId, underlayIfcCfg) = getData(id)
        try {
            underlayAccess.put(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, data: Config, writeContext: WriteContext) {
        val ifcName = formatIfcName(id.firstKeyOf(OcInterface::class.java).interfaceId.value)
        val underlayId = getId(ifcName)
        try {
            underlayAccess.delete(underlayId)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(id: InstanceIdentifier<Config>):
            Pair<InstanceIdentifier<Interface>, Interface> {
        val ifcName = formatIfcName(id.firstKeyOf(OcInterface::class.java).interfaceId.value)
        val underlayIfcCfg = InterfaceBuilder()
                .setName(Interface.Name(ifcName))
                .build()
        return Pair(getId(ifcName), underlayIfcCfg)
    }

    companion object {
        fun getId(ifcName: String): InstanceIdentifier<Interface> =
                RsvpInterfaceReader.RSVP.child(Interface::class.java, InterfaceKey(Interface.Name(ifcName)))

        /**
         * 'set protocols rsvp interface ae100' results in creating interface ae100.0
         * This method enforces '.0' at the end of each interface name unless it's not
         * specified explicitly.
         */
        fun formatIfcName(ifcName: String): String {
            if (ifcName.contains("."))
                return ifcName
            return ifcName.plus(".0")
        }
    }
}