/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.logging.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.um._interface.cfg.rev190610.Interfaces as UmInterfaces
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.um._interface.cfg.rev190610.interfaces.Interface as UmInterface
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.um._interface.cfg.rev190610.interfaces.InterfaceKey as UmInterfaceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.um._interface.cfg.rev190610.interfaces.InterfaceBuilder as UmInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.um._interface.cfg.rev190610.group.body.LoggingBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.um._interface.cfg.rev190610.group.body.logging.EventsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.um._interface.cfg.rev190610.group.body.logging.events.LinkStatusBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.interfaces.structural.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class LoggingInterfacesConfigWriter(private val underlayAccess: UnderlayAccess) :
    WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: IID<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        require(dataAfter.interfaceId.value.startsWith("Bundle-Ether")) {
            "${dataAfter.interfaceId.value} Physical interface is not supported"
        }
        val underlayId = getId(id)
        val underlayBefore = underlayAccess.read(underlayId)
            .checkedGet()
            .get()

        val ifcBuilder = when (underlayBefore) {
            null -> UmInterfaceBuilder(underlayBefore)
            else -> UmInterfaceBuilder()
        }.apply {
            key = UmInterfaceKey(InterfaceName(dataAfter.interfaceId.value))
            interfaceName = InterfaceName(dataAfter.interfaceId.value)
            logging = LoggingBuilder().apply {
                events = EventsBuilder().apply {
                    linkStatus = LinkStatusBuilder().build()
                }.build()
            }.build()
        }

        underlayAccess.safePut(underlayId, ifcBuilder.build())
    }

    override fun updateCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val underlayId = getId(id)
        val before = underlayAccess.read(underlayId)
            .checkedGet()
            .get()
        val builder = UmInterfaceBuilder(before).apply {
            logging = null
        }
        underlayAccess.put(underlayId, builder.build())
    }

    private fun getId(id: IID<Config>): IID<UmInterface> {
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).interfaceId.value)
        val ifcCfg = org.opendaylight.yangtools.yang.binding.InstanceIdentifier.create(UmInterfaces::class.java)!!
        return ifcCfg.child(UmInterface::class.java, UmInterfaceKey(ifcName))
    }
}