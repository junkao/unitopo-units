/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.interfaces

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MDSalReadFailed
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class InterfaceConfigReader(private val underlayAccess: UnderlayAccess) : ReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(instanceIdentifier: IID<Config>): ConfigBuilder {
        return ConfigBuilder()
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(instanceIdentifier: IID<Config>,
                                       configBuilder: ConfigBuilder,
                                       readContext: ReadContext) {
        try {
            val name = instanceIdentifier.firstKeyOf(Interface::class.java).name

            // Getting all configurations and filtering here due to:
            //  - interfaces in underlay are keyed by: name + state compared to only ifc name in openconfig models
            //  - the read is performed in multiple places and with caching its for free
            InterfaceReader.readInterface(underlayAccess, name, handler = { configBuilder.fromUnderlay(it) })
        } catch (e: MDSalReadFailed) {
            throw ReadFailedException(instanceIdentifier, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as InterfaceBuilder).config = config
    }
}

fun ConfigBuilder.fromUnderlay(underlay: InterfaceConfiguration) {
    type = parseIfcType(underlay.interfaceName.value)
    name = underlay.interfaceName.value
    description = underlay.description
    isEnabled = underlay.isShutdown == null
    // FIXME MTU
}

internal fun parseIfcType(name: String): Class<out InterfaceType> {
    // FIXME duplicate with ios-interface-unit
    return when {
        name.startsWith("MgmtEth") -> EthernetCsmacd::class.java
        name.startsWith("FastEther") -> EthernetCsmacd::class.java
        name.startsWith("GigabitEthernet") -> EthernetCsmacd::class.java
        name.startsWith("Loopback") -> SoftwareLoopback::class.java
        else -> Other::class.java
    }
}
