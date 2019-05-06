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

package io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.ifc.base.handler.subinterfaces.AbstractSubinterfaceConfigReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.extention.rev181204.Config1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.extention.rev181204.Config1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.extention.rev181204.RpmTypes
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.EnableDisable
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.rpm.rpm_or_twamp.Case1 as RpmOrTwampCase1
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.rpm.rpm_or_twamp.case_1.RpmType as JunosRpmType
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.rpm.rpm_or_twamp.case_1.rpm.type.Case3 as JunosRpmTypeCase3

class SubinterfaceConfigReader(underlayAccess: UnderlayAccess)
    : AbstractSubinterfaceConfigReader<JunosInterfaceUnit>(underlayAccess) {

    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val ifcName = instanceIdentifier.firstKeyOf(Interface::class.java).name
        val subIfcIndex = instanceIdentifier.firstKeyOf(Subinterface::class.java).index

        readData(underlayAccess.read(readIid(ifcName, subIfcIndex),
            LogicalDatastoreType.CONFIGURATION).checkedGet().orNull(),
            configBuilder, ifcName, subIfcIndex)
    }

    override fun readData(data: JunosInterfaceUnit?, configBuilder: ConfigBuilder, ifcName: String, subIfcIndex: Long) {
        val config1Builder = Config1Builder()
        data?.let {
            configBuilder.fromUnderlay(it)
            config1Builder.fromUnderlay(it)
            configBuilder.addAugmentation(Config1::class.java, config1Builder.build())
        }
    }

    override fun readIid(ifcName: String, subIfcIndex: Long): InstanceIdentifier<JunosInterfaceUnit> =
        InterfaceReader.JUNOS_IFCS.child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
            .child(JunosInterfaceUnit::class.java, UnitKey(subIfcIndex.toString()))

    private fun parseEnableDisable(enableDisable: EnableDisable?): Boolean {
        return when (enableDisable) {
            null -> true
            else -> false
        }
    }

    private fun ConfigBuilder.fromUnderlay(junosUnit: JunosInterfaceUnit) {
        index = junosUnit.name.toLong()
        isEnabled = parseEnableDisable(junosUnit.enableDisable)
        description = junosUnit.description
    }

    private fun Config1Builder.fromUnderlay(junosUnit: JunosInterfaceUnit) {
        val rpmOrTwamp = junosUnit.rpm?.rpmOrTwamp

        when (rpmOrTwamp) {
            is RpmOrTwampCase1 -> rpmOrTwampCase1(rpmOrTwamp.rpmType)
        }
    }

    private fun Config1Builder.rpmOrTwampCase1(junosRpmType: JunosRpmType) {
        when (junosRpmType) {
            is JunosRpmTypeCase3 -> this.rpmType = RpmTypes.ClientDelegateProbes
        }
    }
}