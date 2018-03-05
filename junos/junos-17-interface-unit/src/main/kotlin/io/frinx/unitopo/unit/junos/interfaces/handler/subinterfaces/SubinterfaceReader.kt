/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.interfaces.handler.subinterfaces

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedException
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.UnitKey as JunosInterfaceUnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Interfaces as JunosInterfaces
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey as JunosInterfaceKey

class SubinterfaceReader(private val underlayAccess: UnderlayAccess) : ConfigListReaderCustomizer<Subinterface, SubinterfaceKey, SubinterfaceBuilder> {

    @Throws(ReadFailedException::class)
    override fun getAllIds(iid: InstanceIdentifier<Subinterface>, context: ReadContext): List<SubinterfaceKey> {
        val ifcName = iid.firstKeyOf(Interface::class.java).name
        try {
            return getSubInterfaceIds(underlayAccess, ifcName)
        } catch (e: MdSalReadFailedException) {
            throw ReadFailedException(iid, e)
        }
    }

    private fun getSubInterfaceIds(underlayAccess: UnderlayAccess, ifcName: String): List<SubinterfaceKey> {
        val instanceIdentifier = InterfaceReader.IFCS.child(JunosInterface::class.java,
                JunosInterfaceKey(ifcName))

        return underlayAccess.read(instanceIdentifier, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()
                ?.let { parseSubInterfaceIds(it) }.orEmpty()
    }

    private fun parseSubInterfaceIds(it: JunosInterface): List<SubinterfaceKey> {
        return it.unit.orEmpty().map { it.key }.map { SubinterfaceKey(it.name?.toLong()) }.toList()
    }


    override fun readCurrentAttributes(iid: InstanceIdentifier<Subinterface>, builder: SubinterfaceBuilder, context: ReadContext) {
        try {
            val name = iid.firstKeyOf(Interface::class.java).name
            builder.key = SubinterfaceKey(iid.firstKeyOf(Subinterface::class.java).index)

            InterfaceReader.readUnitCfg(underlayAccess,
                                        name,
                                        iid.firstKeyOf(Subinterface::class.java).index,
                                        { builder.fromUnderlay(it) })

        } catch (e: MdSalReadFailedException) {
            throw ReadFailedException(iid, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, subinterfaces: List<Subinterface>) {
        (builder as SubinterfacesBuilder).subinterface = subinterfaces
    }

    override fun getBuilder(p0: InstanceIdentifier<Subinterface>): SubinterfaceBuilder {
        return SubinterfaceBuilder()
    }

}

private fun SubinterfaceBuilder.fromUnderlay(junosUnit: JunosInterfaceUnit) {
    key = SubinterfaceKey(junosUnit.name.toLong())
}

