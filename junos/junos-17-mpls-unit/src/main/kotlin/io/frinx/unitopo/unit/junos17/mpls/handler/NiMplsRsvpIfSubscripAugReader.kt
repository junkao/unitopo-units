/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos17.mpls.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.mpls.common.MplsReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.extension.rev171024.MplsRsvpSubscriptionConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.extension.rev171024.NiMplsRsvpIfSubscripAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.extension.rev171024.NiMplsRsvpIfSubscripAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.mpls.rsvp.subscription.subscription.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te._interface.attributes.Interface as OcInterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te._interface.attributes.InterfaceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedException

class NiMplsRsvpIfSubscripAugReader(private val underlayAccess: UnderlayAccess) : MplsReader.MplsConfigReader<NiMplsRsvpIfSubscripAug, NiMplsRsvpIfSubscripAugBuilder> {

    override fun merge(p0: Builder<out DataObject>, p1: NiMplsRsvpIfSubscripAug) {
        (p0 as ConfigBuilder).addAugmentation(NiMplsRsvpIfSubscripAug::class.java, p1)
    }

    override fun getBuilder(p0: InstanceIdentifier<NiMplsRsvpIfSubscripAug>): NiMplsRsvpIfSubscripAugBuilder = NiMplsRsvpIfSubscripAugBuilder()

    override fun readCurrentAttributesForType(instanceIdentifier: InstanceIdentifier<NiMplsRsvpIfSubscripAug>, configBuilder: NiMplsRsvpIfSubscripAugBuilder, readContext: ReadContext) {
        val name = RsvpInterfaceConfigWriter.formatIfcName(instanceIdentifier.firstKeyOf<OcInterface, InterfaceKey>(OcInterface::class.java).interfaceId.value)
        try {
            RsvpInterfaceConfigReader.readInterface(underlayAccess, name)?.let {
                it.bandwidth?.let {
                    configBuilder.bandwidth = MplsRsvpSubscriptionConfig.Bandwidth(translateBw(it))
                }
            }
        } catch (e: MdSalReadFailedException) {
            throw ReadFailedException(instanceIdentifier, e)
        }
    }

    private fun translateBw(bandwidth : String) : Long {
        return when {
            bandwidth.endsWith('k') -> bandwidth.removeSuffix("k").toLong() * 1000
            bandwidth.endsWith('m') -> bandwidth.removeSuffix("m").toLong() * 1000000
            bandwidth.endsWith('g') -> bandwidth.removeSuffix("g").toLong() * 1000000000
            else -> bandwidth.toLong()
        }
    }
}