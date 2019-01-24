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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressKey as Ipv4AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.AddressKey as Ipv6AddressKey
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r150730.Ipv6AddressReader as Ipv6AddressRev150730Reader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r170303.Ipv6AddressReader as Ipv6AddressRev170303Reader

class SubinterfaceReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Subinterface, SubinterfaceKey, SubinterfaceBuilder> {

    override fun getAllIds(id: InstanceIdentifier<Subinterface>, context: ReadContext): MutableList<SubinterfaceKey> {
        val ifcName = id.firstKeyOf(Interface::class.java).name

        return if (InterfaceReader.interfaceExists(underlayAccess, id)) {

            // TODO We are misusing the InterfaceReader.getInterfaceIds
            // function. We should create own getSubinterfaceIds function
            // so we can write UT and filter subinterfaces already out of
            // underlay ifc list.
            val subIfcKeys = InterfaceReader.getInterfaceIds(underlayAccess)
                    .filter { InterfaceReader.isSubinterface(it.name) }
                    .filter { it.name.startsWith(ifcName) }
                    .map { InterfaceReader.getSubinterfaceKey(it.name) }

            val ipv4Keys = mutableListOf<Ipv4AddressKey>()
            InterfaceReader.readInterfaceCfg(underlayAccess, ifcName,
                { Ipv4AddressReader.extractAddresses(it, ipv4Keys) })

            val ipv6Keys = mutableListOf<Ipv6AddressKey>()
            InterfaceReader.readInterfaceCfg(underlayAccess, ifcName,
                { Ipv6AddressRev150730Reader.extractAddresses(it, ipv6Keys) })
            InterfaceReader.readInterfaceCfg(underlayAccess, ifcName,
                { Ipv6AddressRev170303Reader.extractAddresses(it, ipv6Keys) })

            return if (!ipv4Keys.isEmpty() || !ipv6Keys.isEmpty())
                subIfcKeys.plus(SubinterfaceKey(ZERO_SUBINTERFACE_ID)).toMutableList() else
                subIfcKeys.toMutableList()
        } else {
            emptyList<SubinterfaceKey>().toMutableList()
        }
    }

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Subinterface>,
        builder: SubinterfaceBuilder,
        ctx: ReadContext
    ) {
        builder.index = id.firstKeyOf(Subinterface::class.java).index
    }

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<Subinterface>) {
        (builder as SubinterfacesBuilder).subinterface = readData
    }

    override fun getBuilder(p0: InstanceIdentifier<Subinterface>): SubinterfaceBuilder = SubinterfaceBuilder()

    companion object {
        const val ZERO_SUBINTERFACE_ID = 0L
    }
}