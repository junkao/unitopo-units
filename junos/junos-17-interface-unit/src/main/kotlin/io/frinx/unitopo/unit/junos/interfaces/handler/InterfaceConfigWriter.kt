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
package io.frinx.unitopo.unit.junos.interfaces.handler

import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.InterfacesType
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.enable.disable.Case1Builder
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceBuilder as JunosInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey as JunosInterfaceKey

class InterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (underlayId, underlayIfcCfg) = getData(id, dataAfter)

        try {
            underlayAccess.put(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val (_, underlayId) = getUnderlayId(id)

        try {
            underlayAccess.delete(underlayId)
        } catch (e: Exception) {
            throw ReadFailedException(id, e)
        }
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        // same as write - preserve existing data and override changed.
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<JunosInterface>, JunosInterface> {
        checkPreconditions(dataAfter, id)
        val (ifcName, underlayId) = getUnderlayId(id)
        val ifcBuilder = InterfaceReader.createBuilderFromExistingInterface(underlayAccess, ifcName)
        setJunosInterfaceBuilder(dataAfter, id, ifcBuilder)
        return Pair(underlayId, ifcBuilder.build())
    }

    private fun setJunosInterfaceBuilder(
        dataAfter: Config,
        id: InstanceIdentifier<Config>,
        ifcBuilder: JunosInterfaceBuilder
    ) {
        val (ifcName, _) = getUnderlayId(id)
        if (dataAfter.shutdown())
            ifcBuilder.enableDisable = Case1Builder().setDisable(true).build()
        else
            ifcBuilder.enableDisable = Case1Builder().setDisable(null).build()
        if (dataAfter.mtu != null)
            ifcBuilder.mtu = InterfacesType.Mtu(dataAfter.mtu.toLong())
        else
            ifcBuilder.mtu = null
        ifcBuilder.name = ifcName
        ifcBuilder.description = dataAfter.description
    }

    private fun checkPreconditions(dataAfter: Config, id: InstanceIdentifier<Config>) {
        val (ifcName, underlayId) = getUnderlayId(id)
        if (!isIfaceNameAndTypeValid(ifcName, dataAfter.type)) {
            throw WriteFailedException(underlayId, String.format("Provided type: {} doesn't match interface name: {}",
                    dataAfter.type, ifcName))
        }
    }

    private fun isIfaceNameAndTypeValid(ifcName: String, type: Class<out InterfaceType>?): Boolean {
        return when (type) {
            EthernetCsmacd::class.java -> isEthernetCsmaCd(ifcName)
            SoftwareLoopback::class.java -> ifcName.startsWith("lo")
            Ieee8023adLag::class.java -> ifcName.startsWith("ae")
            Other::class.java -> true
            else -> false
        }
    }

    private fun getUnderlayId(id: InstanceIdentifier<Config>): Pair<String, InstanceIdentifier<JunosInterface>> {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val underlayId = InterfaceReader.IFCS.child(JunosInterface::class.java, JunosInterfaceKey(ifcName))

        return Pair(ifcName, underlayId)
    }

    private fun Config.shutdown() = isEnabled == null || !isEnabled

    private fun isEthernetCsmaCd(ifcName: String): Boolean {
        return ifcName.startsWith("em") ||  // Management and internal Ethernet interfaces.
            ifcName.startsWith("et") ||     // 100-Gigabit Ethernet interfaces.
            ifcName.startsWith("fe") ||     // Fast Ethernet interface.
            ifcName.startsWith("fxp") ||    // Management and internal Ethernet interfaces.
            ifcName.startsWith("ge") ||     // Gigabit Ethernet interface.
            ifcName.startsWith("xe") // 10-Gigabit Ethernet interface.
    }
}