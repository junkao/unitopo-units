/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.frinx.unitopo.unit.xr6.platform.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev161222.platform.component.top.components.Component
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev161222.platform.component.top.components.ComponentBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev161222.platform.component.top.components.component.Config
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev161222.platform.component.top.components.component.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class ComponentConfigReader : ReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(id: InstanceIdentifier<Config>) = ConfigBuilder()

    override fun readCurrentAttributes(id: InstanceIdentifier<Config>, builder: ConfigBuilder,
                                       ctx: ReadContext) {
        builder.name = id.firstKeyOf(Component::class.java).name
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as ComponentBuilder).config = readValue
    }
}
