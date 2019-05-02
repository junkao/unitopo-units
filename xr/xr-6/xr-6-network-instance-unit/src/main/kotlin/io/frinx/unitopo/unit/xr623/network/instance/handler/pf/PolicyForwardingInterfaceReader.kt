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

import io.frinx.unitopo.ni.base.handler.pf.AbstractPolicyForwardingInterfaceReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.Util
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev161223.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev161223.qos.Qos
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.Optional

class PolicyForwardingInterfaceReader(underlayAccess: UnderlayAccess) :
    AbstractPolicyForwardingInterfaceReader<InterfaceConfigurations>(underlayAccess) {

    override val readIid: InstanceIdentifier<InterfaceConfigurations> = IFC_CFGS

    override fun parseKeys(data: InterfaceConfigurations?): List<InterfaceKey> =
            data?.interfaceConfiguration
            ?.filter { isSupportedInterface(it.interfaceName.value) }
            ?.filter { hasAnyPolicy(it) }
            ?.map { InterfaceKey(InterfaceId(it.interfaceName.value)) }
            .orEmpty()

    companion object {
        val IFC_CFGS = InstanceIdentifier.create(InterfaceConfigurations::class.java)!!

        fun isSupportedInterface(name: String): Boolean {
            return when {
                !name.startsWith("Bundle-Ether") -> false
                Util.isSubinterface(name) -> false
                else -> true
            }
        }

        private fun hasAnyPolicy(configuration: InterfaceConfiguration): Boolean {
            val qos = configuration.getAugmentation(InterfaceConfiguration1::class.java)?.qos

            return when {
                getInputPolicy(qos).isPresent -> true
                getOutputPolicy(qos).isPresent -> true
                else -> false
            }
        }

        fun getInputPolicy(qos: Qos?): Optional<String> {
            return Optional.ofNullable(qos?.input?.servicePolicy?.getOrNull(0)?.servicePolicyName)
        }

        fun getOutputPolicy(qos: Qos?): Optional<String> {
            return Optional.ofNullable(qos?.output?.servicePolicy?.getOrNull(0)?.servicePolicyName)
        }
    }
}