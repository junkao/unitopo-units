/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class SubinterfaceConfigReader(private val underlayAccess: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as SubinterfaceBuilder).config = config
    }

    override fun readCurrentAttributes(id: InstanceIdentifier<Config>,
                                       builder: ConfigBuilder,
                                       readContext: ReadContext) {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val subifcIndex = id.firstKeyOf(Subinterface::class.java).index

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