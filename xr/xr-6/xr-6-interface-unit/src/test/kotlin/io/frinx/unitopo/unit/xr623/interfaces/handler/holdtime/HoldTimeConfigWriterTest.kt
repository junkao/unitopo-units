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

package io.frinx.unitopo.unit.xr623.interfaces.handler.holdtime

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.drivers.media.eth.cfg.rev150730.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.drivers.media.eth.cfg.rev150730._interface.configurations._interface.configuration.Ethernet
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.drivers.media.eth.cfg.rev150730._interface.configurations._interface.configuration.ethernet.CarrierDelay
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.drivers.media.eth.cfg.rev150730._interface.configurations._interface.configuration.ethernet.CarrierDelayBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.HoldTime
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class HoldTimeConfigWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext

    @Mock
    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: HoldTimeConfigWriter

    companion object {
        private val NATIVE_IF_NAME_UPDATE = InterfaceName(HoldTimeConfigReaderTest.IF_NAME)
        private val NATIVE_IF_NAME_WRITE = InterfaceName("GigabitEthernet0/0/0/1")
        private val NATIVE_ACT = InterfaceActive("act")
        private val NATIVE_IID_WRITE = InterfaceReader.IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME_WRITE))
            .augmentation(InterfaceConfiguration1::class.java)
            .child(Ethernet::class.java)
            .child(CarrierDelay::class.java)
        private val NATIVE_IID_UPDATE = InterfaceReader.IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME_UPDATE))
            .augmentation(InterfaceConfiguration1::class.java)
            .child(Ethernet::class.java)
            .child(CarrierDelay::class.java)
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(HoldTimeConfigReaderTest.NC_HELPER))
        target = HoldTimeConfigWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val ifName = "GigabitEthernet0/0/0/1"
        val id = IIDs.INTERFACES
            .child(Interface::class.java, InterfaceKey(ifName))
            .child(HoldTime::class.java)
            .child(Config::class.java)
        val config = ConfigBuilder()
            .setUp(HoldTimeConfigReaderTest.UP_VALUE)
            .setDown(HoldTimeConfigReaderTest.DOWN_VALUE)
            .build()
        val expectedUnderlay = CarrierDelayBuilder()
            .setCarrierDelayUp(config.up)
            .setCarrierDelayDown(config.down)
            .build()
        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<DataObject>

        Mockito.doNothing().`when`(underlayAccess).safePut(Mockito.any(), Mockito.any())
        // test
        target.writeCurrentAttributes(id, config, writeContext)
        // capture
        Mockito.verify(underlayAccess, Mockito.times(1))
                .safePut(idCap.capture(), dataCap.capture())
        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))
        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID_WRITE) as Matcher<in InstanceIdentifier<DataObject>>
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(expectedUnderlay) as Matcher<in DataObject>
        )
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val dataBefore = ConfigBuilder()
            .setUp(HoldTimeConfigReaderTest.UP_VALUE)
            .setDown(HoldTimeConfigReaderTest.DOWN_VALUE)
            .build()
        val dataAfter = ConfigBuilder()
            .setUp(HoldTimeConfigReaderTest.UP_VALUE + 100)
            .setDown(HoldTimeConfigReaderTest.DOWN_VALUE + 100)
            .build()
        val underlayBefore = CarrierDelayBuilder()
            .setCarrierDelayUp(dataBefore.up)
            .setCarrierDelayDown(dataBefore.down)
            .build()
        val underlayAfter = CarrierDelayBuilder()
            .setCarrierDelayUp(dataBefore.up + 100)
            .setCarrierDelayDown(dataBefore.down + 100)
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<DataObject>
        val idCap1 = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>
        val dataCap1 = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<DataObject>

        Mockito.doNothing().`when`(underlayAccess).safeMerge(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())
        target.updateCurrentAttributes(HoldTimeConfigReaderTest.IID_CONFIG, dataBefore, dataAfter, writeContext)

        Mockito.verify(underlayAccess, Mockito.times(1))
            .safeMerge(idCap.capture(), dataCap.capture(), idCap1.capture(), dataCap1.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(idCap1.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap1.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID_UPDATE) as Matcher<in InstanceIdentifier<DataObject>>
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(underlayBefore) as Matcher<in DataObject>
        )
        Assert.assertThat(
            idCap1.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID_UPDATE) as Matcher<in InstanceIdentifier<DataObject>>
        )
        Assert.assertThat(
            dataCap1.allValues[0],
            CoreMatchers.equalTo(underlayAfter) as Matcher<in DataObject>
        )
    }

    @Test
    fun testDeleteCurrentAttributes() {
        Mockito.doNothing().`when`(underlayAccess).safeDelete(Mockito.any(), Mockito.any())
        val dataBefore = ConfigBuilder()
            .setUp(HoldTimeConfigReaderTest.UP_VALUE)
            .setDown(HoldTimeConfigReaderTest.DOWN_VALUE)
            .build()
        target.deleteCurrentAttributes(HoldTimeConfigReaderTest.IID_CONFIG, dataBefore, writeContext)
        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<DataObject>
        Mockito.verify(underlayAccess, Mockito.times(1)).safeDelete(idCap.capture(), dataCap.capture())
        Assert.assertEquals(1, idCap.allValues.size)
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID_UPDATE) as Matcher<in InstanceIdentifier<DataObject>>
        )
    }
}