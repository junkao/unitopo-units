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

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev161223.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev161223.qos.Qos
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev161223.qos.QosBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev161223.qos.qos.InputBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev161223.qos.qos.OutputBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev161223.service.policy.ServicePolicy
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.qos.ma.cfg.rev161223.service.policy.ServicePolicyBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.cisco.rev171109.NiPfIfCiscoAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.cisco.rev171109.NiPfIfCiscoAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.policy.forwarding.top.PolicyForwarding
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class PolicyForwardingInterfaceConfigWriterTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: PolicyForwardingInterfaceConfigWriter

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        // open config
        private val IF_NAME = "Bundle-Ether65533"
        private val IF_INPUT = "input_test"
        private val IF_OUTPUT = "output_test"
        val niPfIfCiscoAugBuilder = NiPfIfCiscoAugBuilder()
            .setInputServicePolicy(IF_INPUT)
            .setOutputServicePolicy(IF_OUTPUT)
            .build()

        val IID_CONFIG = InstanceIdentifier
            .create(NetworkInstances::class.java)
            .child(NetworkInstance::class.java, NetworkInstanceKey("default"))
            .child(PolicyForwarding::class.java)
            .child(Interfaces::class.java)
            .child(Interface::class.java, InterfaceKey(InterfaceId(IF_NAME)))
            .child(Config::class.java)

        private val CONFIG = ConfigBuilder()
            .setInterfaceId(InterfaceId(IF_NAME))
            .addAugmentation(NiPfIfCiscoAug::class.java, niPfIfCiscoAugBuilder)
            .build()

        // netconf
        private val NATIVE_IF_NAME = InterfaceName(IF_NAME)
        private val NATIVE_ACT = InterfaceActive("act")
        val pfIfAug: NiPfIfCiscoAug = CONFIG.getAugmentation(NiPfIfCiscoAug::class.java)
        val intputBuilder = InputBuilder()
        val outputBuilder = OutputBuilder()
        val servicePolicyBuilder = ServicePolicyBuilder()
        val qosBuilder = QosBuilder()
        val servicepolicy_input = servicePolicyBuilder.setServicePolicyName(pfIfAug.inputServicePolicy).build()
        val servicepolicy_output = servicePolicyBuilder.setServicePolicyName(pfIfAug.outputServicePolicy).build()
        val list_input = listOf<ServicePolicy>(servicepolicy_input)
        val list_output = listOf<ServicePolicy>(servicepolicy_output)
        private val NATIVE_IID: InstanceIdentifier<Qos> =
            InstanceIdentifier.create(InterfaceConfigurations::class.java)
                .child(InterfaceConfiguration::class.java,
                    InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))
                .augmentation(InterfaceConfiguration1::class.java)
                .child(Qos::class.java)
        private val NATIVE_CONFIG = qosBuilder
            .setInput(intputBuilder.setServicePolicy(list_input).build())
            .setOutput(outputBuilder.setServicePolicy(list_output).build())
            .build()
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = PolicyForwardingInterfaceConfigWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val config = ConfigBuilder(CONFIG)
            .build()

        val expectedConfig = QosBuilder(NATIVE_CONFIG)
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<InterfaceConfiguration>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<InterfaceConfiguration>

        Mockito.doNothing().`when`(underlayAccess).safePut(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(IID_CONFIG, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).safePut(idCap.capture(),
            dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<InterfaceConfiguration>>
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(expectedConfig) as Matcher<in InterfaceConfiguration>
        )
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val configBefore = ConfigBuilder(CONFIG).build()
        val configAfter = ConfigBuilder(CONFIG).build()
        Mockito.doNothing().`when`(underlayAccess).safeMerge(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())

        // test
        target.updateCurrentAttributes(IID_CONFIG, configBefore, configAfter, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).safeMerge(Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any())
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val config = ConfigBuilder(CONFIG).build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<InterfaceConfiguration>>

        Mockito.doNothing().`when`(underlayAccess).delete(Mockito.any())

        // test
        target.deleteCurrentAttributes(IID_CONFIG, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).delete(idCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<InterfaceConfiguration>>
        )
    }
}