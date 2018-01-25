/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos17.mpls.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.mpls.common.MplsListReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te.InterfaceAttributesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te._interface.attributes.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te._interface.attributes.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te._interface.attributes.InterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Configuration
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Protocols
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.Rsvp
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class RsvpInterfaceReader(private val underlayAccess: UnderlayAccess) : MplsListReader.MplsConfigListReader<Interface, InterfaceKey, InterfaceBuilder> {

    override fun merge(p0: Builder<out DataObject>, p1: MutableList<Interface>) {
        (p0 as InterfaceAttributesBuilder).`interface` = p1
    }

    override fun readCurrentAttributesForType(instanceIdentifier: InstanceIdentifier<Interface>, interfaceBuilder: InterfaceBuilder, readContext: ReadContext) {
        val key = instanceIdentifier.firstKeyOf(Interface::class.java)
        interfaceBuilder.interfaceId = key.interfaceId
    }

    override fun getBuilder(p0: InstanceIdentifier<Interface>): InterfaceBuilder = InterfaceBuilder()

    override fun getAllIdsForType(id: InstanceIdentifier<Interface>, readContext: ReadContext): List<InterfaceKey> = getInterfaceIds(underlayAccess)

    companion object {

        val RSVP = InstanceIdentifier.create(Configuration::class.java)
                .child(Protocols::class.java)
                .child(Rsvp::class.java)!!

        fun getInterfaceIds(underlayAccess: UnderlayAccess): List<InterfaceKey> {
            return underlayAccess.read(RSVP, LogicalDatastoreType.OPERATIONAL)
                    .checkedGet()
                    .orNull()
                    ?.let { parseInterfaceIds(it) }.orEmpty()
        }

        @VisibleForTesting
        fun parseInterfaceIds(rsvp: Rsvp): List<InterfaceKey> {
            return rsvp.`interface`.orEmpty().map {
                InterfaceKey(InterfaceId(it.name.interfaceName.value))
            }.toList()
        }
    }
}