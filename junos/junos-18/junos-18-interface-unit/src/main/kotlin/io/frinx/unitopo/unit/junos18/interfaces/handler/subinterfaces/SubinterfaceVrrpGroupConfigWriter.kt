/*
 * Copyright © 2019 Frinx and others.
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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ip.vrrp.top.vrrp.vrrp.group.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ip.vrrp.top.vrrp.VrrpGroup
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.Address
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.common.types.rev180101.Ipv4addr
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.common.types.rev180101.Ipv4prefix
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.AddressKey as JunosInterfaceUnitFamilyInetAddressKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitKey as JunosInterfaceUnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.Family as JunosInterfaceUnitFamily
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.Inet as JunosInterfaceUnitFamilyInet
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.address.VrrpGroup as JunosVrrpGroup
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.Address as JunosInterfaceUnitFamilyInetAddress
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.address.VrrpGroupBuilder as JunosVrrpGroupBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.address.VrrpGroupKey as JunosVrrpGroupKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.vrrp.group.address.Case1Builder

open class SubinterfaceVrrpGroupConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {
    override fun writeCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {

        val prefixLen = underlayAccess.read(getUnderlayInetId(id),
                LogicalDatastoreType.CONFIGURATION)
                .checkedGet().orNull().let {
                    it?.address?.filter {
                        it.name.value.split("/")?.get(0) == id.firstKeyOf(Address::class.java).ip.value
                    }?.get(0)?.name?.value?.split("/")?.get(1)
                }
        val underlayId = getUnderlayId(id, prefixLen!!)
        underlayAccess.put(underlayId, getData(id, dataAfter))
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, config: Config, context: WriteContext) {
        val prefixLen = underlayAccess.read(getUnderlayInetId(id),
                LogicalDatastoreType.CONFIGURATION)
                .checkedGet().orNull().let {
                    it?.address?.filter {
                        it.name.value.split("/")?.get(0) == id.firstKeyOf(Address::class.java).ip.value
                    }?.get(0)?.name?.value?.split("/")?.get(1)
                }
        val underlayVrrpGroupId = getUnderlayId(id, prefixLen!!)
        underlayAccess.delete(underlayVrrpGroupId)
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
            JunosVrrpGroup {
        val builder = JunosVrrpGroupBuilder().apply {
            name = dataAfter.virtualRouterId.toLong()
            val cb1 = Case1Builder()
            cb1.virtualAddress = dataAfter.virtualAddress?.map {
                Ipv4addr(it.ipv4Address.value)
            }
            address = cb1.build()
        }
        return builder.build()
    }

    private fun getUnderlayId(id: InstanceIdentifier<Config>, len: String): InstanceIdentifier<JunosVrrpGroup> {

        val ifcName = id.firstKeyOf(Interface::class.java).name
        val vrrpGroupKey = id.firstKeyOf(VrrpGroup::class.java)
        val jVrrpGK = JunosVrrpGroupKey(vrrpGroupKey.virtualRouterId.toLong())
        val underlayUnitName = id.firstKeyOf(Subinterface::class.java).index.toString()
        val addreskey = id.firstKeyOf(Address::class.java)
        val prefix = Ipv4prefix(addreskey.ip.value + "/" + len)
        val jAddresKey = JunosInterfaceUnitFamilyInetAddressKey(prefix)

        return InterfaceReader.JUNOS_IFCS
                .child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
                .child(JunosInterfaceUnit::class.java, JunosInterfaceUnitKey(underlayUnitName))
                .child(JunosInterfaceUnitFamily::class.java)
                .child(JunosInterfaceUnitFamilyInet::class.java)
                .child(JunosInterfaceUnitFamilyInetAddress::class.java, jAddresKey)
                .child(JunosVrrpGroup::class.java, jVrrpGK)
    }

    private fun getUnderlayInetId(id: InstanceIdentifier<Config>): InstanceIdentifier<JunosInterfaceUnitFamilyInet> {

        val ifcName = id.firstKeyOf(Interface::class.java).name
        val vrrpGroupKey = id.firstKeyOf(VrrpGroup::class.java)
        val underlayUnitName = id.firstKeyOf(Subinterface::class.java).index.toString()

        return InterfaceReader.JUNOS_IFCS
                .child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
                .child(JunosInterfaceUnit::class.java, JunosInterfaceUnitKey(underlayUnitName))
                .child(JunosInterfaceUnitFamily::class.java)
                .child(JunosInterfaceUnitFamilyInet::class.java)
    }
}