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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class PolicyForwardingInterfaceConfigWriter(private val underlayAccess: UnderlayAccess) :
    WriterCustomizer<Config> {
    override fun writeCurrentAttributes(
        instanceIdentifier: IID<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        requires(instanceIdentifier)
        val pfIfAug = dataAfter.getAugmentation(NiPfIfCiscoAug::class.java)
        pfIfAug ?: return

        val inputBuilder = InputBuilder()
        val outputBuilder = OutputBuilder()

        // to update, must read now underlay config
        val ifcName = instanceIdentifier.firstKeyOf(Interface::class.java).interfaceId.value
        val ifcConfig = underlayAccess.read(
            PolicyForwardingInterfaceReader.IFC_CFGS,
            LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
            ?.let {
                it.interfaceConfiguration.orEmpty()
                    .firstOrNull { it.interfaceName.value == ifcName }
            }
        val qosBuilder = getQosBuilder(ifcConfig)

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

        val qos = qosBuilder.build()
        underlayAccess.put(getId(instanceIdentifier), qos)
    }

    override fun updateCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        requires(id)
        val underlayId = getId(id)
        underlayAccess.delete(underlayId)
    }

    private fun getId(id: IID<Config>): IID<Qos> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).interfaceId.value)
        val IFC_CFGS = IID.create(InterfaceConfigurations::class.java)
        val underlayId = IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
            .augmentation(InterfaceConfiguration1::class.java)
            .child(Qos::class.java)
        return underlayId
    }

    private fun requires(id: IID<Config>) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        require(vrfName == NetworInstance.DEFAULT_NETWORK_NAME) {
            "Policy-forwarding is only supported in default network-instance, but was $vrfName"
        }

        val ifcName = id.firstKeyOf(Interface::class.java).interfaceId.value
        require(PolicyForwardingInterfaceReader.isSupportedInterface(ifcName)) {
            "Policy-forwarding is supported in Bundle-Ether and Physical interface, but now is $ifcName"
        }
    }

    private fun getQosBuilder(underlay: InterfaceConfiguration?): QosBuilder {
        val qos = underlay?.getAugmentation(InterfaceConfiguration1::class.java)?.qos
        return when (qos) {
            null -> QosBuilder()
            else -> QosBuilder(qos)
        }
    }
}