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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceModeEnum
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier

class InterfaceConfigWriterTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: InterfaceConfigWriter

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        private val IF_NAME = "Bundle-Ether300"
        private val IF_DESCRIPTION = "IF_DESCRIPTION-001"
        private val IF_TYPE = Ieee8023adLag::class.java

        private val NATIVE_ACT = InterfaceActive("act")
        private val NATIVE_IF_MODE_NON_PHYS: InterfaceModeEnum? = null
        private val NATIVE_IF_NAME = InterfaceName(IF_NAME)

        private val IID_CONFIG = KeyedInstanceIdentifier
                .create(Interfaces::class.java)
                .child(Interface::class.java, InterfaceKey(IF_NAME))
                .child(Config::class.java)

        private val CONFIG = ConfigBuilder()
                .setName(IF_NAME)
                .setDescription(IF_DESCRIPTION)
                .setType(IF_TYPE)
                .build()

        private val NATIVE_IID: KeyedInstanceIdentifier<InterfaceConfiguration, InterfaceConfigurationKey> =
                KeyedInstanceIdentifier
                        .create(InterfaceConfigurations::class.java)
                        .child(InterfaceConfiguration::class.java,
                            InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))

        private val NATIVE_CONFIG = InterfaceConfigurationBuilder()
                .setActive(NATIVE_ACT)
                .setDescription(IF_DESCRIPTION)
                .setInterfaceVirtual(true)
                .setInterfaceName(NATIVE_IF_NAME)
                .setInterfaceModeNonPhysical(NATIVE_IF_MODE_NON_PHYS)
                .build()
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = Mockito.spy(InterfaceConfigWriter(underlayAccess))
    }

    @Test
    fun testWriteCurrentAttributes() {
        val config = ConfigBuilder(CONFIG) // not customize
                .build()
        val expectedConfig = InterfaceConfigurationBuilder(NATIVE_CONFIG) // not customize
                .build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<InterfaceConfiguration>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<InterfaceConfiguration>

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(IID_CONFIG, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).put(idCap.capture(), dataCap.capture())

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
                CoreMatchers.equalTo(expectedConfig)
        )
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val config = ConfigBuilder(CONFIG) // not customize
                .build()

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

    @Test
    fun testUpdateCurrentAttributes() {
        val configBefore = ConfigBuilder(CONFIG) // not customize
                .build()
        val configAfter = ConfigBuilder(CONFIG)
                .setDescription(CONFIG.description + "-after")
                .build()
        val expectedConfig = InterfaceConfigurationBuilder(NATIVE_CONFIG)
                .setDescription(configAfter.description)
                .build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<InterfaceConfiguration>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<InterfaceConfiguration>

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.updateCurrentAttributes(IID_CONFIG, configBefore, configAfter, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).put(idCap.capture(), dataCap.capture())

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
                CoreMatchers.equalTo(expectedConfig)
        )
    }
}