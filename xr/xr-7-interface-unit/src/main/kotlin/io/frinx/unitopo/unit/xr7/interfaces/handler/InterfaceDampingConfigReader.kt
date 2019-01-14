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

package io.frinx.unitopo.unit.xr7.interfaces.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations._interface.configuration.Dampening
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.DampingBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.damping.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.damping.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class InterfaceDampingConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {
    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Config>,
        builder: ConfigBuilder,
        readContext: ReadContext
    ) {

        val ifcName = instanceIdentifier.firstKeyOf(Interface::class.java).name
        if (!ifcName.startsWith("Bundle-Ether")) {
            InterfaceReader.readInterfaceCfg(underlayAccess, ifcName, { extractDamping(it, builder) })
        }
    }

    fun extractDamping(ifcCfg: InterfaceConfiguration, builder: ConfigBuilder) {
        ifcCfg.let {
            it.dampening?.let {
                builder.fromUnderlay(it)
            }
        }
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as DampingBuilder).setConfig(readValue)
    }

    override fun getBuilder(p0: InstanceIdentifier<Config>): ConfigBuilder {
        return ConfigBuilder()
    }
}

fun ConfigBuilder.fromUnderlay(dampening: Dampening) {
    maxSuppress = dampening.suppressThreshold
    suppress = dampening.suppressTime
    reuse = dampening.reuseThreshold
    halfLife = dampening.halfLife
    isEnabled = true
}