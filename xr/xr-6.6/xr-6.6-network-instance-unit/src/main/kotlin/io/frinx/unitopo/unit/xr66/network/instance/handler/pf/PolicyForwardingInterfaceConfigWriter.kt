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

package io.frinx.unitopo.unit.xr66.network.instance.handler.pf

import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.ni.base.handler.pf.AbstractPolicyForwardingInterfaceConfigWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev180227.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev180227.qos.Qos
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev180227.qos.QosBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev180227.qos.qos.InputBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev180227.qos.qos.OutputBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev180227.service.policy.ServicePolicyBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.cisco.rev171109.NiPfIfCiscoAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class PolicyForwardingInterfaceConfigWriter(underlayAccess: UnderlayAccess) :
    AbstractPolicyForwardingInterfaceConfigWriter<Qos>(underlayAccess) {

    override fun getUnderlayIid(iid: InstanceIdentifier<Config>): InstanceIdentifier<Qos> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(iid.firstKeyOf(Interface::class.java).interfaceId.value)
        return InstanceIdentifier.create(InterfaceConfigurations::class.java)
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
            .augmentation(InterfaceConfiguration1::class.java)
            .child(Qos::class.java)
    }

    override fun getData(data: Config): Qos {
        val qosBuilder = QosBuilder()
        val pfIfAug = data.getAugmentation(NiPfIfCiscoAug::class.java)
        pfIfAug ?: return qosBuilder.build()

        val inputBuilder = InputBuilder()
        val outputBuilder = OutputBuilder()
        pfIfAug.inputServicePolicy?.let {
            qosBuilder.setInput(inputBuilder
                .setServicePolicy(
                    listOf(ServicePolicyBuilder().setServicePolicyName(it).build())
                )
                .build()
            )
        }
        pfIfAug.outputServicePolicy?.let {
            qosBuilder.setOutput(outputBuilder
                .setServicePolicy(
                    listOf(ServicePolicyBuilder().setServicePolicyName(it).build())
                )
                .build()
            )
        }
        return qosBuilder.build()
    }

    override fun requires(id: InstanceIdentifier<Config>) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        require(vrfName == NetworInstance.DEFAULT_NETWORK_NAME) {
            "Policy-forwarding is only supported in default network-instance, but was $vrfName"
        }

        val ifcName = id.firstKeyOf(Interface::class.java).interfaceId.value
        require(PolicyForwardingInterfaceReader.isSupportedInterface(ifcName)) {
            "Policy-forwarding is supported in Bundle-Ether and Physical interface, but now is $ifcName"
        }
    }
}