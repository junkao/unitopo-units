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

package io.frinx.unitopo.unit.junos.snmp.handler

import com.google.common.base.Optional
import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.event.types.rev171024.LINKUPDOWN
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.snmp.rev171024.snmp.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Configuration
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.traps.choice.NoTrapsBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.traps.choice.TrapsBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Interfaces
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

    override fun updateCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeData(id, dataAfter)
    }

    private fun resolveOcInterface(id: IID<Config>, writeContext: WriteContext):
        Pair<OcInterfaceKey, Optional<OcInterface>> {
        val ocIfcKey = OcInterfaceKey(id.firstKeyOf(SnmpInterface::class.java).interfaceId.value)
        val ocIfcOpt = writeContext.readAfter(IIDs.INTERFACES.child(OcInterface::class.java, ocIfcKey))
        return Pair(ocIfcKey, ocIfcOpt)
    }

    private fun writeData(id: IID<Config>, data: Config?) {
        val (underlayId, underlayIfcCfg) = getData(id, data)
        underlayAccess.put(underlayId, underlayIfcCfg)
    }

    private fun getData(id: IID<Config>, dataAfter: Config?): Pair<IID<Interface>, Interface> {
        val (ifcName, underlayId) = getUnderlayId(id)
        val ifcOpt = underlayAccess.read(underlayId, LogicalDatastoreType.CONFIGURATION).checkedGet().orNull()

        val ifcBuilder = InterfaceBuilder()
        ifcOpt?.let { ifcBuilder.fieldsFrom(it) }

        ifcBuilder.key = InterfaceKey(ifcName)
        val isLinkUpDown = dataAfter?.enabledTrapForEvent?.firstOrNull()?.eventName == LINKUPDOWN::class.java
        val isEnabled = dataAfter?.enabledTrapForEvent?.firstOrNull()?.isEnabled ?: false

        if (isLinkUpDown) {
            if (isEnabled) {
                ifcBuilder.trapsChoice = TrapsBuilder().setTraps(true).build()
            } else {
                ifcBuilder.trapsChoice = NoTrapsBuilder().setNoTraps(true).build()
            }
        } else {
            ifcBuilder.trapsChoice = null
        }

        return Pair(underlayId, ifcBuilder.build())
    }

    companion object {
        fun getUnderlayId(id: IID<Config>): Pair<String, IID<Interface>> {
            val ifcName = id.firstKeyOf(SnmpInterface::class.java).interfaceId.value
            val iid = IID.create(Configuration::class.java)
                    .child(Interfaces::class.java)
                    .child(Interface::class.java, InterfaceKey(ifcName))
            return Pair(ifcName, iid)
        }
    }
}