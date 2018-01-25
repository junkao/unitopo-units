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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.extension.rev171024.NiMplsRsvpIfSubscripAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.extension.rev171024.NiMplsRsvpIfSubscripAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te._interface.attributes.Interface as OcInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.rsvp.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.rsvp.InterfaceBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NiMplsRsvpIfSubscripAugWriter (private val underlayAccess: UnderlayAccess) : WriterCustomizer<NiMplsRsvpIfSubscripAug> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<NiMplsRsvpIfSubscripAug>, p1: NiMplsRsvpIfSubscripAug, p2: WriteContext) {
        val (underlayId, underlayIfcCfg) = getData(id, p1)
        try {
            underlayAccess.put(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<NiMplsRsvpIfSubscripAug>, p1: NiMplsRsvpIfSubscripAug, p2: WriteContext) {
        writeCurrentAttributes(id, NiMplsRsvpIfSubscripAugBuilder().build(), p2)
    }

    private fun getData(id: InstanceIdentifier<NiMplsRsvpIfSubscripAug>, data: NiMplsRsvpIfSubscripAug):
            Pair<InstanceIdentifier<Interface>, Interface> {
        val ifcName = RsvpInterfaceConfigWriter.formatIfcName(id.firstKeyOf(OcInterface::class.java).interfaceId.value)
        val underlayIfcCfg = InterfaceBuilder()
            .setName(Interface.Name(ifcName))
        data.bandwidth?.let {
            underlayIfcCfg.setBandwidth(data.bandwidth.toString())
        }
        return Pair(RsvpInterfaceConfigWriter.getId(ifcName), underlayIfcCfg.build())
    }
}
