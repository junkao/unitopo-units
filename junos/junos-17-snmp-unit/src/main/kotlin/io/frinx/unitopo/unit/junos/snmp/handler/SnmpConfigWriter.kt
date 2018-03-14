/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.snmp.handler

import com.google.common.base.Optional
import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.traps.choice.NoTrapsBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.traps.choice.TrapsBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface as OcInterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey as OcInterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces.Interface as SnmpInterface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class SnmpConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: IID<Config>, dataAfter: Config, writeContext: WriteContext) {
        val (ocIfcKey, ocIfcOpt) = resolveOcInterface(id, writeContext)
        Preconditions.checkArgument(ocIfcOpt.isPresent, "SNMP cannot be configured because " +
                "interface ${ocIfcKey.name} does not exist.")
        writeData(id, dataAfter)
    }

    override fun deleteCurrentAttributes(id: IID<Config>, dataBefore: Config, writeContext: WriteContext) {
        writeData(id, null)
    }

    override fun updateCurrentAttributes(id: IID<Config>, dataBefore: Config, dataAfter: Config, writeContext: WriteContext) {
        writeData(id, dataAfter)
    }

    private fun resolveOcInterface(id: IID<Config>, writeContext: WriteContext): Pair<OcInterfaceKey, Optional<OcInterface>> {
        val ocIfcKey = OcInterfaceKey(id.firstKeyOf(SnmpInterface::class.java).interfaceId.value)
        val ocIfcOpt = writeContext.readAfter(IIDs.INTERFACES.child(OcInterface::class.java, ocIfcKey))
        return Pair(ocIfcKey, ocIfcOpt)
    }

    private fun writeData(id: IID<Config>, data: Config?) {
        val (underlayId, underlayIfcCfg) = getData(id, data)
        underlayAccess.put(underlayId, underlayIfcCfg)
    }

    private fun getData(id: IID<Config>, dataAfter: Config?):
            Pair<IID<Interface>, Interface> {
        val (ifcName, underlayId) = SnmpConfigReader.getUnderlayId(id)
        val ifcOpt = underlayAccess.read(underlayId, LogicalDatastoreType.CONFIGURATION).checkedGet()
        val ifcBuilder = if (ifcOpt.isPresent) {
            InterfaceBuilder(ifcOpt.get())
        } else {
            InterfaceBuilder()
        }

        ifcBuilder.setKey(InterfaceKey(ifcName))
                .let {
                    if (dataAfter != null
                            && dataAfter.enabledTrapForEvent != null
                            && !dataAfter.enabledTrapForEvent!!.isEmpty()) {
                        it.setTrapsChoice(TrapsBuilder().setTraps(true).build())
                    } else {
                        it.setTrapsChoice(NoTrapsBuilder().setNoTraps(true).build())
                    }
                }
                .build()
        return Pair(underlayId, ifcBuilder.build())
    }

}