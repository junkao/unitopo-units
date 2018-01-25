/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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
        fun formatIfcName(ifcName: String) : String {
            if (ifcName.contains("."))
                return ifcName
            return ifcName.plus(".0")
        }
    }
}