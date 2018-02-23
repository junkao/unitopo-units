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

class TeInterfaceReader(private val underlayAccess: UnderlayAccess) : MplsListReader.MplsConfigListReader<Interface, InterfaceKey, InterfaceBuilder> {

    override fun getAllIdsForType(instanceIdentifier: InstanceIdentifier<Interface>, readContext: ReadContext): List<InterfaceKey> {
        try {
            return getInterfaceIds(underlayAccess)
        } catch (e: org.opendaylight.controller.md.sal.common.api.data.ReadFailedException) {
            throw ReadFailedException(instanceIdentifier, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, readData: List<Interface>) {
        (builder as TeInterfaceAttributesBuilder).`interface` = readData
    }

    override fun readCurrentAttributesForType(instanceIdentifier: InstanceIdentifier<Interface>, interfaceBuilder: InterfaceBuilder, readContext: ReadContext) {
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