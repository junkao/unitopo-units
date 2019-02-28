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

package io.frinx.unitopo.unit.xr7.network.instance.policy.forwarding

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev180227.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.cisco.rev171109.NiPfIfCiscoAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.cisco.rev171109.NiPfIfCiscoAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces._interface.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class PolicyForwardingInterfaceConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {
    override fun getBuilder(instanceIdentifier: IID<Config>): ConfigBuilder {
        return ConfigBuilder()
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as InterfaceBuilder).config = config
    }

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val ifcName = instanceIdentifier.firstKeyOf(Interface::class.java).interfaceId.value

        PolicyForwardingInterfaceReader.readInterfaceCfg(underlayAccess, ifcName) { configBuilder.fromUnderlay(it) }
    }
}

private fun ConfigBuilder.fromUnderlay(underlay: InterfaceConfiguration) {
    val qos = underlay.getAugmentation(InterfaceConfiguration1::class.java).qos

    val niPfIfCiscoAugBuilder = NiPfIfCiscoAugBuilder()
    PolicyForwardingInterfaceReader.getInputPolicy(qos).ifPresent { niPfIfCiscoAugBuilder.inputServicePolicy = it }
    PolicyForwardingInterfaceReader.getOutputPolicy(qos).ifPresent { niPfIfCiscoAugBuilder.outputServicePolicy = it }

    addAugmentation(NiPfIfCiscoAug::class.java, niPfIfCiscoAugBuilder.build())
    interfaceId = InterfaceId(underlay.interfaceName.value)
}