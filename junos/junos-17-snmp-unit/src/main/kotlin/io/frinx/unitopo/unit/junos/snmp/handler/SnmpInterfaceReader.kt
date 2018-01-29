/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.snmp.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces.Interface as SnmpInterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces.InterfaceBuilder as SnmpInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces.InterfaceKey as SnmpInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Configuration
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.traps.choice.Traps
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Interfaces
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class SnmpInterfaceReader(private val underlayAccess: UnderlayAccess) : ConfigListReaderCustomizer<SnmpInterface, SnmpInterfaceKey, SnmpInterfaceBuilder> {

    override fun getBuilder(id: IID<SnmpInterface>): SnmpInterfaceBuilder = SnmpInterfaceBuilder()

    override fun merge(builder: Builder<out DataObject>, iface: List<SnmpInterface>) {
        (builder as InterfacesBuilder).`interface` = iface
    }

    override fun getAllIds(id: IID<SnmpInterface>, readContext: ReadContext): List<SnmpInterfaceKey> {
        return readInterfaceIds()
    }

    override fun readCurrentAttributes(id: IID<SnmpInterface>, builder: InterfaceBuilder, readContext: ReadContext) {
        val ifcId = id.firstKeyOf(SnmpInterface::class.java).interfaceId
        builder.key = SnmpInterfaceKey(ifcId)
    }

    private fun readInterfaceIds(): List<SnmpInterfaceKey> {

        return underlayAccess.read(IID.create(Configuration::class.java)
                .child(Interfaces::class.java), LogicalDatastoreType.CONFIGURATION)
                .checkedGet().orNull()
                ?.`interface`
                ?.filter {
                    it.trapsChoice != null && it.trapsChoice is Traps
                }
                ?.map {
                    SnmpInterfaceKey(InterfaceId(it.name))
                }
                ?.toList().orEmpty()
    }

}