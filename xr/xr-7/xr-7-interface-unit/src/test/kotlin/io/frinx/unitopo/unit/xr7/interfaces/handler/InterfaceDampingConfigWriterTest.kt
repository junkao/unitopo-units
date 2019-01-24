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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations._interface.configuration.Dampening
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations._interface.configuration.DampeningBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.Interface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.Damping
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.damping.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.damping.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceDampingConfigWriterTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: InterfaceDampingConfigWriter

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes2.xml")
        private val DATA_NODES = getResourceAsString(javaClass, "/data_nodes2.xml")
        val IFC_CFGS = InstanceIdentifier.create(InterfaceConfigurations::class.java)!!
        // Open Config
        private val IF_NAME = "GigabitEthernet0/0/0/0"
        private val IF_HALFLIFE = 10
        private val IF_REUSE = 11
        private val IF_MAXSUPPRESS = 13

        // netconf
        private val NATIVE_IF_NAME = InterfaceName(IF_NAME)
        private val NATIVE_ACT = InterfaceActive("act")

        private val IID_CONFIG = InstanceIdentifier
            .create(Interfaces::class.java)
            .child(Interface::class.java, InterfaceKey(IF_NAME)).augmentation(Interface1::class.java)
            .child(Damping::class.java).child(Config::class.java)

        private val NATIVE_IID: InstanceIdentifier<Dampening> = IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))
            .child(Dampening::class.java)

        private val NATIVE_CONFIG = DampeningBuilder()
            .setHalfLife(IF_HALFLIFE.toLong())
            .setReuseThreshold(IF_REUSE.toLong())
            .setSuppressTime(IF_MAXSUPPRESS.toLong())
            .setSuppressThreshold(IF_MAXSUPPRESS.toLong())
            .setArgs(Dampening.Args.SpecifyAll)
            .build()
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(InterfaceDampingConfigWriterTest.NC_HELPER))
        target = InterfaceDampingConfigWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val config = ConfigBuilder().apply {
            this.isEnabled = true
            this.setHalfLife(IF_HALFLIFE.toLong())
            this.setReuse(IF_REUSE.toLong())
            this.setSuppress(IF_MAXSUPPRESS.toLong())
            this.setMaxSuppress(IF_MAXSUPPRESS.toLong())
        }.build()
        val expectedConfig = DampeningBuilder(NATIVE_CONFIG).build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<InterfaceConfiguration>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<InterfaceConfiguration>

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(IID_CONFIG, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).put(idCap.capture(),
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
    fun testDeleteCurrentAttributes() {
        val config = ConfigBuilder().apply {
            this.setHalfLife(IF_HALFLIFE.toLong())
            this.setReuse(IF_REUSE.toLong())
            this.setSuppress(IF_MAXSUPPRESS.toLong())
            this.setMaxSuppress(IF_MAXSUPPRESS.toLong())
        }.build()
        val expectedConfig = DampeningBuilder(NATIVE_CONFIG) // not customize
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
        val configBefore = ConfigBuilder().apply {
            this.isEnabled = true
            this.setHalfLife(IF_HALFLIFE.toLong())
            this.setReuse(IF_REUSE.toLong())
            this.setSuppress(IF_MAXSUPPRESS.toLong())
            this.setMaxSuppress(IF_MAXSUPPRESS.toLong())
        }.build()
        val configAfter = ConfigBuilder().apply {
            this.isEnabled = true
            this.setHalfLife(10.toLong())
            this.setReuse(11.toLong())
            this.setSuppress(13.toLong())
            this.setMaxSuppress(13.toLong())
        }.build()
        val expectedConfig = DampeningBuilder(NATIVE_CONFIG)
            .setHalfLife(IF_HALFLIFE.toLong())
            .setReuseThreshold(IF_REUSE.toLong())
            .setSuppressTime(IF_MAXSUPPRESS.toLong())
            .setSuppressThreshold(IF_MAXSUPPRESS.toLong())
            .build()

        val data_Cfg = parseGetCfgResponse(DATA_NODES, NATIVE_IID)
        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<InterfaceConfiguration>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<InterfaceConfiguration>

        Mockito.doNothing().`when`(underlayAccess).merge(Mockito.any(), Mockito.any())
        // test
        target.updateCurrentAttributes(IID_CONFIG, configBefore, configAfter, writeContext)
        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).merge(idCap.capture(), dataCap.capture())
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
}