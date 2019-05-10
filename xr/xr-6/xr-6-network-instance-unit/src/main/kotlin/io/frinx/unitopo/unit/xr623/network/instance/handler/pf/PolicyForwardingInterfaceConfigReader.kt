/*
 * Copyright © 2019 Frinx and others.
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

package io.frinx.unitopo.unit.xr623.network.instance.handler.pf

import io.frinx.unitopo.ni.base.handler.pf.AbstractPolicyForwardingInterfaceConfigReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev161223.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.cisco.rev171109.NiPfIfCiscoAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.cisco.rev171109.NiPfIfCiscoAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces._interface.ConfigBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class PolicyForwardingInterfaceConfigReader(underlayAccess: UnderlayAccess) :
    AbstractPolicyForwardingInterfaceConfigReader<InterfaceConfigurations>(underlayAccess) {

    override val readIid: InstanceIdentifier<InterfaceConfigurations> =
        PolicyForwardingInterfaceReader.IFC_CFGS

    override fun readData(data: InterfaceConfigurations?, ifcName: String, builder: ConfigBuilder) {
        data?.let { interfaceConfigurations ->
            interfaceConfigurations.interfaceConfiguration.orEmpty()
                .firstOrNull { it.interfaceName.value == ifcName }
                .let { builder.fromUnderlay(it ?: getDefaultIfcCfg(ifcName)) }
        }
    }

    private fun getDefaultIfcCfg(name: String): InterfaceConfiguration {
        return InterfaceConfigurationBuilder().apply {
            interfaceName = InterfaceName(name)
            isShutdown = null
        }.build()
    }

    private fun ConfigBuilder.fromUnderlay(underlay: InterfaceConfiguration) {
        val qos = underlay.getAugmentation(InterfaceConfiguration1::class.java).qos

        val niPfIfCiscoAugBuilder = NiPfIfCiscoAugBuilder()
        PolicyForwardingInterfaceReader.getInputPolicy(qos).ifPresent {
            niPfIfCiscoAugBuilder.inputServicePolicy = it
        }
        PolicyForwardingInterfaceReader.getOutputPolicy(qos).ifPresent {
            niPfIfCiscoAugBuilder.outputServicePolicy = it
        }

        addAugmentation(NiPfIfCiscoAug::class.java, niPfIfCiscoAugBuilder.build())
        interfaceId = InterfaceId(underlay.interfaceName.value)
    }
}