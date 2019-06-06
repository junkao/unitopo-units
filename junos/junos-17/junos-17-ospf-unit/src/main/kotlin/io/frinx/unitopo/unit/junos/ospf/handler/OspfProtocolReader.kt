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
package io.frinx.unitopo.unit.junos.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.builder.Check
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.OSPF
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Areaid
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Configuration
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Protocols
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.Ospf
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.AreaKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.Overload
import java.util.Collections
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.Area as JunosArea
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.area.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.area.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfProtocolReader(private val underlayAccess: UnderlayAccess) :
    CompositeListReader.Child<Protocol, ProtocolKey, ProtocolBuilder> {

    override fun getCheck(): Check {
        return ChecksMap.PathCheck.Protocol.OSPF
    }

    override fun readCurrentAttributes(id: IID<Protocol>, proto: ProtocolBuilder, readContext: ReadContext) {
        proto.key = ProtocolKey(OSPF::class.java, OSPF_INSTANCE_DEFAULT)
    }

    override fun getAllIds(id: IID<Protocol>, readContext: ReadContext): List<ProtocolKey> {
        val ospf = underlayAccess.read(getOspfId()).checkedGet()
        if (ospf.isPresent) {
            return Collections.singletonList(ProtocolKey(OSPF::class.java, OSPF_INSTANCE_DEFAULT))
        }
        return emptyList()
    }

    companion object {
        private const val OSPF_INSTANCE_DEFAULT = "default"

        fun getOspfId(): IID<Ospf> = IID.create(Configuration::class.java)
            .child(Protocols::class.java)
            .child(Ospf::class.java)

        fun getOspfOverloadId(): IID<Overload> = getOspfId()
            .child(Overload::class.java)

        fun getAreaId(area: String): IID<JunosArea> = getOspfId()
            .child(JunosArea::class.java, AreaKey(Areaid(area)))

        fun getInterfaceId(area: String, iface: String): IID<JunosInterface> = getAreaId(area)
            .child(JunosInterface::class.java, JunosInterfaceKey(JunosInterface.Name(iface)))
    }
}