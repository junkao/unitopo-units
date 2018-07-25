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
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.SubinterfaceReader.Companion.ZERO_SUBINTERFACE_ID
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class SubinterfaceConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as SubinterfaceBuilder).config = config
    }

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Config>,
        builder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val subifcIndex = id.firstKeyOf(Subinterface::class.java).index

        // Only parse configuration for non 0 subifc
        if (subifcIndex == ZERO_SUBINTERFACE_ID) {
            builder.index = ZERO_SUBINTERFACE_ID
            return
        }

        // TODO set this in ConfigBuilder.fromUnderlay extension
        builder.index = subifcIndex
        val subifcName = getSubIfcName(ifcName, subifcIndex)
        InterfaceReader.readInterfaceCfg(underlayAccess, subifcName, { builder.fromUnderlay(it) })
    }

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<Config>) = ConfigBuilder()
}

private fun ConfigBuilder.fromUnderlay(underlay: InterfaceConfiguration) {
    name = underlay.interfaceName.value
    description = underlay.description
    isEnabled = underlay.isShutdown == null
}