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
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader.Companion.LAG_PREFIX
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.EnableDisable
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.enable.disable.Case1
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MDSalReadFailed
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class InterfaceConfigReader(private val underlayAccess: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(instanceIdentifier: IID<Config>): ConfigBuilder = ConfigBuilder()

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(instanceIdentifier: IID<Config>,
                                       configBuilder: ConfigBuilder,
                                       readContext: ReadContext) {
        try {
            val name = instanceIdentifier.firstKeyOf(Interface::class.java).name.removePrefix(LAG_PREFIX)
            InterfaceReader.readInterfaceCfg(underlayAccess, name, { configBuilder.fromUnderlay(it) })
        } catch (e: MDSalReadFailed) {
            throw ReadFailedException(instanceIdentifier, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as InterfaceBuilder).config = config
    }
}

internal fun ConfigBuilder.fromUnderlay(underlay: JunosInterface) {
    val ifcType = parseIfcType(underlay.name)

    name = when(ifcType){
        Ieee8023adLag::class.java -> LAG_PREFIX + underlay.name
        else -> underlay.name
    }
    description = underlay.description
    type = ifcType
    mtu = underlay.mtu?.uint32?.toInt()
    isEnabled = parseEnableDisable(underlay.enableDisable)
}

internal fun parseIfcType(name: String): Class<out InterfaceType>? {
    return when {
        name.startsWith("em") -> EthernetCsmacd::class.java
        name.startsWith("et") -> EthernetCsmacd::class.java
        name.startsWith("fe") -> EthernetCsmacd::class.java
        name.startsWith("fxp") -> EthernetCsmacd::class.java
        name.startsWith("ge") -> EthernetCsmacd::class.java
        name.startsWith("xe") -> EthernetCsmacd::class.java
        name.startsWith("lo") -> SoftwareLoopback::class.java
        name.startsWith("ae") -> Ieee8023adLag::class.java
        else -> Other::class.java
    }
}

internal fun parseEnableDisable(enableDisable: EnableDisable?): Boolean? {
    return when (enableDisable) {
        null -> true
        is Case1 -> false
        else -> false
    }
}



