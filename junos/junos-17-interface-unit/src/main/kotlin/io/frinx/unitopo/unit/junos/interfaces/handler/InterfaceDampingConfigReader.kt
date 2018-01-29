/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.interfaces.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.DampingBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.damping.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.damping.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MDSalReadFailed
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Damping as JunosDamping
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.DampingBuilder as JunosDampingBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class InterfaceDampingConfigReader(private val underlayAccess: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder> {
    override fun getBuilder(iid: IID<Config>): ConfigBuilder {
        return ConfigBuilder()
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(iid: IID<Config>, builder: ConfigBuilder, context: ReadContext) {
        try {
            val name = iid.firstKeyOf(Interface::class.java).name
            InterfaceReader.readDampingCfg(underlayAccess, name, { builder.fromUnderlay(it) })
        } catch (e: MDSalReadFailed) {
            throw ReadFailedException(iid, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, data: Config) {
        (builder as DampingBuilder).config = data
    }
}

internal fun ConfigBuilder.fromUnderlay(underlay: JunosDamping?) {
    isEnabled = parseIsEnabled(underlay?.isEnable)
    halfLife = underlay?.halfLife?.uint32
    reuse = underlay?.reuse?.uint32
    suppress = underlay?.suppress?.uint32
    maxSuppress = underlay?.maxSuppress?.uint32
}

internal fun parseIsEnabled(isEnabled: Boolean?): Boolean? {
    return when (isEnabled) {
        null -> false
        else -> true
    }
}
