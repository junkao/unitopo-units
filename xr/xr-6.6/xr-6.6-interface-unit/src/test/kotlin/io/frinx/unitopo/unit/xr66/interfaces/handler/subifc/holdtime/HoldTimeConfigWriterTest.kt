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

package io.frinx.unitopo.unit.xr66.interfaces.handler.subifc.holdtime

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev180615.InterfaceConfiguration6
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev180615._interface.configurations._interface.configuration.CarrierDelay
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev180615._interface.configurations._interface.configuration.CarrierDelayBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.IfSubifCiscoHoldTimeAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.HoldTime
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.Subinterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class HoldTimeConfigWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: HoldTimeConfigWriter

    private lateinit var idCap: ArgumentCaptor<IID<DataObject>>
    private lateinit var dataCap: ArgumentCaptor<DataObject>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = HoldTimeConfigWriter(underlayAccess)

        idCap = ArgumentCaptor.forClass(IID::class.java) as ArgumentCaptor<IID<DataObject>>
        dataCap = ArgumentCaptor.forClass(DataObject::class.java)
    }

    @Test
    fun writeCurrentAttributes() {
        val config = ConfigBuilder(CONFIG)
                .build()
        val expectedConfig = CarrierDelayBuilder(NATIVE_CONFIG)
                .build()

        Mockito.doNothing().`when`(underlayAccess).merge(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(IID_CONFIG, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).merge(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(idCap.allValues[0], CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<DataObject>>)
        Assert.assertThat(dataCap.allValues[0], CoreMatchers.equalTo(expectedConfig) as Matcher<DataObject>)
    }

    @Test
    fun deleteCurrentAttributes() {
        val config = ConfigBuilder(CONFIG)
                .build()
        val expectedConfig = CarrierDelayBuilder() // empty
                .build()

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.deleteCurrentAttributes(IID_CONFIG, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(idCap.allValues[0], CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<DataObject>>)
        Assert.assertThat(dataCap.allValues[0], CoreMatchers.equalTo(expectedConfig) as Matcher<DataObject>)
    }

    @Test
    fun updateCurrentAttributes() {
        val configBefore = Mockito.mock(Config::class.java)
        val config = ConfigBuilder(CONFIG)
                .build()
        val expectedConfig = CarrierDelayBuilder(NATIVE_CONFIG)
                .build()

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.updateCurrentAttributes(IID_CONFIG, configBefore, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(idCap.allValues[0], CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<DataObject>>)
        Assert.assertThat(dataCap.allValues[0], CoreMatchers.equalTo(expectedConfig) as Matcher<DataObject>)
    }

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        private val NATIVE_ACT = InterfaceActive("act")

        private val INTERFACE_NAME = "Bundle-Ether4001"
        private val SUBINTERFACE_INDEX = 10L
        private val UP = 100L
        private val DOWN = 0L

        // OpenConfig
        private val IID_CONFIG = IIDs.INTERFACES
                .child(Interface::class.java, InterfaceKey(INTERFACE_NAME))
                .child(Subinterfaces::class.java)
                .child(Subinterface::class.java, SubinterfaceKey(SUBINTERFACE_INDEX))
                .augmentation(IfSubifCiscoHoldTimeAug::class.java)
                .child(HoldTime::class.java)
                .child(Config::class.java)
        // The down property is not handled in openconfig model.
        private val CONFIG = ConfigBuilder()
                .setUp(UP)
                .build()

        // native(netconf)
        private val NATIVE_IF_NAME = InterfaceName("$INTERFACE_NAME.$SUBINTERFACE_INDEX")
        private val NATIVE_IID = IID.create(InterfaceConfigurations::class.java)
                .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))
                .augmentation(InterfaceConfiguration6::class.java)
                .child(CarrierDelay::class.java)
        private val NATIVE_CONFIG = NC_HELPER.read(NATIVE_IID).get().get()
    }
}