/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.unitopo.handlers.ospf.OspfReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ProtocolsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.OSPF
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Areaid
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Configuration
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Protocols
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.Ospf
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.AreaKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import java.util.*
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.Area as JunosArea
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.area.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.ospf.area.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfProtocolReader(private val underlayAccess: UnderlayAccess) :
        OspfReader.OspfConfigReader<Protocol, ProtocolBuilder>,
        ListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder> {

    override fun getBuilder(id: IID<Protocol>): ProtocolBuilder = ProtocolBuilder()

    override fun merge(builder: Builder<out DataObject>, protocols: List<Protocol>) {
        (builder as ProtocolsBuilder).protocol = protocols
    }

    override fun readCurrentAttributesForType(id: IID<Protocol>, proto: ProtocolBuilder, readContext: ReadContext) {
        proto.key = ProtocolKey(OSPF::class.java, Companion.OSPF_INSTANCE_DEFAULT)
    }

    override fun getAllIds(id: IID<Protocol>, readContext: ReadContext): List<ProtocolKey> {
        val ospf = underlayAccess.read(getOspfId()).checkedGet()
        if (ospf.isPresent) {
            return Collections.singletonList(ProtocolKey(OSPF::class.java, Companion.OSPF_INSTANCE_DEFAULT))
        }
        return emptyList()
    }

    companion object {
        fun getOspfId(): IID<Ospf> {
            return IID.create(Configuration::class.java)
                    .child(Protocols::class.java)
                    .child(Ospf::class.java)
        }

        fun getAreaId(area: String): IID<JunosArea> {
            return getOspfId()
                    .child(JunosArea::class.java, AreaKey(Areaid(area)))
        }

        fun getInterfaceId(area: String, iface: String): IID<JunosInterface> {
            return getAreaId(area)
                    .child(JunosInterface::class.java, JunosInterfaceKey(JunosInterface.Name(iface)))
        }

        private val OSPF_INSTANCE_DEFAULT = "default"
    }
}
