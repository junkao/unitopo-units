/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lldp

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces._interface.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceConfigReader : ReaderCustomizer<Config, ConfigBuilder> {

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as InterfaceBuilder).config = readValue
    }

    override fun readCurrentAttributes(id: InstanceIdentifier<Config>, builder: ConfigBuilder, ctx: ReadContext) {
        builder.name = id.firstKeyOf(Interface::class.java).name
        builder.isEnabled = true
    }

    override fun getBuilder(id: InstanceIdentifier<Config>) = ConfigBuilder()
}