/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.interfaces.handler

import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader.Companion.LAG_PREFIX
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

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext) {
        val (underlayId, underlayIfcCfg) = getData(id, dataAfter)

        try {
            underlayAccess.put(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>,
                                         dataBefore: Config,
                                         writeContext: WriteContext) {
        val (_, underlayId) = getUnderlayId(id)

        try {
            underlayAccess.delete(underlayId)
        } catch (e: Exception) {
            throw ReadFailedException(id, e)
        }
    }

    override fun updateCurrentAttributes(id: InstanceIdentifier<Config>,
                                         dataBefore: Config, dataAfter: Config,
                                         writeContext: WriteContext) {
        val (underlayId, underlayIfcCfg) = getData(id, dataAfter)

        try {
            // Check if enabling the interface from disabled state
            // since enableDisable is an empty leaf, enabling an interface cannot be done with merge
            if (!dataBefore.isEnabled && !dataAfter.shutdown()) {
                val previousStateWithoutShut = getJunosInterfaceBuilder(dataBefore, id).setEnableDisable(null).build()
                underlayAccess.put(underlayId, previousStateWithoutShut)
            }

            underlayAccess.merge(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<JunosInterface>, JunosInterface> {
        val (_, underlayId) = getUnderlayId(id)

        val ifcBuilder = getJunosInterfaceBuilder(dataAfter, id)

        return Pair(underlayId, ifcBuilder.build())
    }

    private fun getJunosInterfaceBuilder(dataAfter: Config, id: InstanceIdentifier<Config>): JunosInterfaceBuilder {
        val (ifcName, underlayId) = getUnderlayId(id)
        val ifcBuilder = JunosInterfaceBuilder()
        if (dataAfter.shutdown()) ifcBuilder.enableDisable = Case1Builder().setDisable(true).build()
        if (!checkInterfaceType(ifcName, dataAfter.type)) {
            throw WriteFailedException(underlayId, String.format("Provided type: {} doesn't match interface name: {}",
                    dataAfter.type, ifcName))
        }
        if (dataAfter.mtu != null) ifcBuilder.mtu = InterfacesType.Mtu(dataAfter.mtu.toLong())
        ifcBuilder.name = ifcName
        ifcBuilder.description = dataAfter.description
        return ifcBuilder
    }

    private fun checkInterfaceType(ifcName: String, type: Class<out InterfaceType>?): Boolean {
        return when(type){
            EthernetCsmacd::class.java -> isEthernetCsmaCd(ifcName)
            SoftwareLoopback::class.java -> ifcName.startsWith("lo")
            Ieee8023adLag::class.java -> ifcName.startsWith("ae")
            Other::class.java -> true
            else -> false
        }
    }

    private fun getUnderlayId(id: InstanceIdentifier<Config>): Pair<String, InstanceIdentifier<JunosInterface>> {
        val ifcName = id.firstKeyOf(Interface::class.java).name.removePrefix(LAG_PREFIX)
        val underlayId = InterfaceReader.IFCS.child(JunosInterface::class.java, JunosInterfaceKey(ifcName))

        return Pair(ifcName, underlayId)
    }

    private fun Config.shutdown() = isEnabled == null || !isEnabled

    private fun isEthernetCsmaCd(ifcName: String): Boolean {
        return ifcName.startsWith("em")         // Management and internal Ethernet interfaces.
                || ifcName.startsWith("et")     // 100-Gigabit Ethernet interfaces.
                || ifcName.startsWith("fe")     // Fast Ethernet interface.
                || ifcName.startsWith("fxp")    // Management and internal Ethernet interfaces.
                || ifcName.startsWith("ge")     // Gigabit Ethernet interface.
                || ifcName.startsWith("xe")     // 10-Gigabit Ethernet interface.
    }
}
