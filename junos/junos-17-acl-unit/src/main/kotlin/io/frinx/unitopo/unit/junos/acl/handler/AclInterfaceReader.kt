/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.acl.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.Interface as AclInterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.InterfaceBuilder as AclInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.InterfaceKey as AclInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Configuration
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Unit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.UnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.Family
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.Inet
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.Filter
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Interfaces
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class AclInterfaceReader(private val underlayAccess: UnderlayAccess) : ConfigListReaderCustomizer<AclInterface, AclInterfaceKey, AclInterfaceBuilder> {

    override fun getBuilder(id: IID<AclInterface>): AclInterfaceBuilder = AclInterfaceBuilder()

    override fun merge(builder: Builder<out DataObject>, iface: List<AclInterface>) {
        (builder as InterfacesBuilder).`interface` = iface
    }

    override fun getAllIds(id: IID<AclInterface>, readContext: ReadContext): List<AclInterfaceKey> {
        return readInterfaceIds()
    }

    override fun readCurrentAttributes(id: IID<AclInterface>, builder: InterfaceBuilder, readContext: ReadContext) {
        val setKey = id.firstKeyOf(AclInterface::class.java)
        builder.key = setKey
    }

    private fun readInterfaceIds(): List<AclInterfaceKey> {

        return underlayAccess.read(IID.create(Configuration::class.java)
                .child(Interfaces::class.java), LogicalDatastoreType.CONFIGURATION)
                .checkedGet().orNull()
                ?.`interface`
                ?.filter {
                    //TODO add support for more virtual interfaces (other than unit 0)
                    //TODO add support for family inet6
                    it?.unit?.get(0)?.family?.inet?.filter != null
                }
                ?.map {
                    AclInterfaceKey(InterfaceId(it.name))
                }
                ?.toList().orEmpty()
    }

    companion object {
        public fun getUnderlayFilterId(ifcName: String): IID<Filter> {
            return IID.create(Configuration::class.java)
                    .child(Interfaces::class.java)
                    .child(Interface::class.java, InterfaceKey(ifcName))
                    //TODO add support for more virtual interfaces (other than unit 0)
                    .child(Unit::class.java, UnitKey("0"))
                    .child(Family::class.java)
                    //TODO add support for family inet6
                    .child(Inet::class.java)
                    .child(Filter::class.java)
        }
    }
}