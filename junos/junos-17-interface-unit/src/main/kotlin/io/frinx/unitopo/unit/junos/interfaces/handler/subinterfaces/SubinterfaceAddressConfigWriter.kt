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

package io.frinx.unitopo.unit.junos.interfaces.handler.subinterfaces

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.address.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Ipv4prefix
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.UnitKey as JunosInterfaceUnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.Family as JunosInterfaceUnitFamily
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.Inet as JunosInterfaceUnitFamilyInet
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.Address as JunosInterfaceUnitFamilyInetAddress
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.AddressBuilder as JunosInterfaceUnitFamilyInetAddressBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.AddressKey as JunosInterfaceUnitFamilyInetAddressKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey as JunosInterfaceKey

class SubinterfaceAddressConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (underlayIfcUnitFamilyInetAddressId, underlayIfcUnitFamilyInetAddress) = getData(id, dataAfter)

        try {
            underlayAccess.put(underlayIfcUnitFamilyInetAddressId, underlayIfcUnitFamilyInetAddress)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, config: Config, context: WriteContext) {
        val ipPrefix = Ipv4prefix(config.ip.value + "/" + config.prefixLength)

        val underlayIfcUnitFamilyInetAddressId = getUnderlayId(id, ipPrefix)

        try {
            underlayAccess.delete(underlayIfcUnitFamilyInetAddressId)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (underlayIfcUnitFamilyInetAddressId, underlayIfcUnitFamilyInetAddress) = getData(id, dataAfter)

        try {
            underlayAccess.merge(underlayIfcUnitFamilyInetAddressId, underlayIfcUnitFamilyInetAddress)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<JunosInterfaceUnitFamilyInetAddress>, JunosInterfaceUnitFamilyInetAddress> {
        val ipPrefix = Ipv4prefix(dataAfter.ip.value + "/" + dataAfter.prefixLength)
        val underlayIfcUnitFamilyInetAddressId = getUnderlayId(id, ipPrefix)

        val ifcUnitFamilyInetAddressBuilder = JunosInterfaceUnitFamilyInetAddressBuilder()
        ifcUnitFamilyInetAddressBuilder.name = ipPrefix

        return Pair(underlayIfcUnitFamilyInetAddressId, ifcUnitFamilyInetAddressBuilder.build())
    }

    private fun getUnderlayId(id: InstanceIdentifier<Config>, ipPrefix: Ipv4prefix):
        InstanceIdentifier<JunosInterfaceUnitFamilyInetAddress> {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val underlayUnitName = id.firstKeyOf(Subinterface::class.java).index.toString()

        return InterfaceReader.IFCS.child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
                .child(JunosInterfaceUnit::class.java, JunosInterfaceUnitKey(underlayUnitName))
                .child(JunosInterfaceUnitFamily::class.java)
                .child(JunosInterfaceUnitFamilyInet::class.java)
                .child(JunosInterfaceUnitFamilyInetAddress::class.java,
                        JunosInterfaceUnitFamilyInetAddressKey(ipPrefix))
    }
}