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
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev180227.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev180227.qos.Qos
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import java.util.Optional
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class PolicyForwardingInterfaceReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {
    override fun getAllIds(instanceIdentifier: IID<Interface>, readContext: ReadContext): List<InterfaceKey> {
        val vrfName = instanceIdentifier.firstKeyOf(NetworkInstance::class.java).name
        if (vrfName != NetworInstance.DEFAULT_NETWORK_NAME) {
            return emptyList()
        }
        return underlayAccess.read(IFC_CFGS, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
            ?.interfaceConfiguration
            ?.filter { isSupportedInterface(it.interfaceName.value) }
            ?.filter { hasAnyPolicy(it) }
            ?.map { InterfaceKey(InterfaceId(it.interfaceName.value)) }
            .orEmpty()
    }

    override fun getBuilder(instanceIdentifier: IID<Interface>): InterfaceBuilder = InterfaceBuilder()

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Interface>,
        interfaceBuilder: InterfaceBuilder,
        readContext: ReadContext
    ) {
        // Just set the name
        interfaceBuilder.interfaceId = instanceIdentifier.firstKeyOf(Interface::class.java).interfaceId
    }

    override fun merge(builder: Builder<out DataObject>, list: List<Interface>) {
        (builder as InterfacesBuilder).`interface` = list
    }

    companion object {
        val IFC_CFGS = IID.create(InterfaceConfigurations::class.java)!!
        fun readInterfaceCfg(
            underlayAccess: UnderlayAccess,
            name: String,
            handler: (InterfaceConfiguration) -> kotlin.Unit
        ) {
            val configurations = underlayAccess.read(IFC_CFGS, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()

            configurations?.let {
                it.interfaceConfiguration.orEmpty()
                    .firstOrNull { it.interfaceName.value == name }
                    .let { handler(it ?: getDefaultIfcCfg(name)) }
            }
        }

        private fun getDefaultIfcCfg(name: String): InterfaceConfiguration {
            return InterfaceConfigurationBuilder().apply {
                interfaceName = InterfaceName(name)
                isShutdown = null
            }.build()
        }

        fun isSupportedInterface(name: String): Boolean {
            return when {
                InterfaceReader.parseIfcType(name) == Other::class.java -> false
                InterfaceReader.isSubinterface(name) -> false
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