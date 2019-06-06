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

package io.frinx.unitopo.unit.xr7.interfaces.handler.aggregate

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceReader
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501.InterfaceConfiguration3 as LacpInterfaceConfig
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.mdrv.lib.cfg.rev151109.InterfaceConfiguration2Builder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.mdrv.lib.cfg.rev151109.InterfaceConfiguration2 as MdrvInterfaceConfig
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501._interface.configurations._interface.configuration.Lacp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501._interface.configurations._interface.configuration.LacpBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.ext.rev180926.IfLagAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.ext.rev180926.IfLagAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Interface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.Aggregation
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class AggregateConfigWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: AggregateConfigWriter

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        private val NATIVE_ACT = InterfaceActive("act")
        private val IF_NAME = "Bundle-Ether3000"
        private val SYSTEM_MAC = MacAddress("00:00:00:00:08:00")
        private val MAC_ADDRESS = MacAddress("02:8a:96:00:00:00")

        private val NATIVE_IF_NAME = InterfaceName(IF_NAME)

        private val IID_CONFIG = IIDs.INTERFACES
                .child(Interface::class.java, InterfaceKey(IF_NAME))
                .augmentation(Interface1::class.java)
                .child(Aggregation::class.java)
                .child(Config::class.java)

        private val ifLagAugBuilder = IfLagAugBuilder()
                .setSystemIdMac(SYSTEM_MAC)
                .setMacAddress(MAC_ADDRESS)

        private val CONFIG = ConfigBuilder()
                .addAugmentation(IfLagAug::class.java, ifLagAugBuilder.build()).build()

        private val NATIVE_IID = InterfaceReader.IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))

        private val DATA_IFC = NC_HELPER.read(NATIVE_IID).checkedGet().get()

        private val NATIVE_IID_SYSTEM_MAC = InterfaceReader.IFC_CFGS
                .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))
                .augmentation(LacpInterfaceConfig::class.java)
                .child(Lacp::class.java)

        private val NATIVE_CONFIG_SYSTEM_MAC = LacpBuilder(DATA_IFC
                .getAugmentation(LacpInterfaceConfig::class.java).lacp)
                .setSystemMac(SYSTEM_MAC)
                .build()

        private val NATIVE_IID_MAC_ADDRESS = InterfaceReader.IFC_CFGS
                .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))

        private val NATIVE_CONFIG_MAC_ADDRESS = InterfaceConfiguration2Builder(DATA_IFC
                .getAugmentation(MdrvInterfaceConfig::class.java))
                .setMacAddr(MAC_ADDRESS)
                .build()
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = AggregateConfigWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val id = IID_CONFIG
        val config = CONFIG

        val expectedConfigLacp = NATIVE_CONFIG_SYSTEM_MAC

        val expectedConfigMacAddr = NATIVE_CONFIG_MAC_ADDRESS

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<DataObject>

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(id, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(2))
                .put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(2))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(2))

        // verify captured values
        // bundle
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID_SYSTEM_MAC) as Matcher<in InstanceIdentifier<DataObject>>
        )
        Assert.assertThat(
                dataCap.allValues[0],
                CoreMatchers.equalTo(expectedConfigLacp) as Matcher<in DataObject>
        )

        // lacp
        Assert.assertThat(
                idCap.allValues[1],
                CoreMatchers.equalTo(NATIVE_IID_MAC_ADDRESS) as Matcher<in InstanceIdentifier<DataObject>>
        )

        Assert.assertThat(
                (dataCap.allValues[1] as InterfaceConfiguration).getAugmentation(MdrvInterfaceConfig::class.java),
                CoreMatchers.equalTo(expectedConfigMacAddr) as Matcher<in DataObject>
        )
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val id = IID_CONFIG
        val config = CONFIG

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<out DataObject>

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.deleteCurrentAttributes(id, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(2))
                .put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(2))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(2))

        // verify captured values
        // bundle
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID_SYSTEM_MAC) as Matcher<in InstanceIdentifier<DataObject>>
        )
        Assert.assertThat(
                dataCap.allValues[0],
                CoreMatchers.equalTo(LacpBuilder().build()) as Matcher<in DataObject>
        )

        // lacp
        Assert.assertThat(
                idCap.allValues[1],
                CoreMatchers.equalTo(NATIVE_IID_MAC_ADDRESS) as Matcher<in InstanceIdentifier<DataObject>>
        )

        Assert.assertThat(
                (dataCap.allValues[1] as InterfaceConfiguration).getAugmentation(MdrvInterfaceConfig::class.java),
                CoreMatchers.equalTo(null) as Matcher<DataObject>
        )
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val id = IID_CONFIG
        val configBefore = CONFIG
        val SYSTEM_MAC_AFT = MacAddress("00:00:00:00:08:09")
        val MAC_ADDRESS_AFT = MacAddress("02:8a:96:00:00:09")
        val ifLagAugBuilder = IfLagAugBuilder()
                .setSystemIdMac(SYSTEM_MAC_AFT)
                .setMacAddress(MAC_ADDRESS_AFT)

        val configAfter = ConfigBuilder()
                .addAugmentation(IfLagAug::class.java, ifLagAugBuilder.build()).build()

        val expectedConfigLacp = LacpBuilder(DATA_IFC
                .getAugmentation(LacpInterfaceConfig::class.java).lacp)
                .setSystemMac(SYSTEM_MAC_AFT)
                .build()

        val expectedConfigMacAddr = InterfaceConfiguration2Builder(DATA_IFC
                .getAugmentation(MdrvInterfaceConfig::class.java))
                .setMacAddr(MAC_ADDRESS_AFT)
                .build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<DataObject>

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.updateCurrentAttributes(id, configBefore, configAfter, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(2))
                .put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(2))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(2))

        // verify captured values
        // bundle
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID_SYSTEM_MAC) as Matcher<in InstanceIdentifier<DataObject>>
        )
        Assert.assertThat(
                dataCap.allValues[0],
                CoreMatchers.equalTo(expectedConfigLacp) as Matcher<in DataObject>
        )

        // lacp
        Assert.assertThat(
                idCap.allValues[1],
                CoreMatchers.equalTo(NATIVE_IID_MAC_ADDRESS) as Matcher<in InstanceIdentifier<DataObject>>
        )

        Assert.assertThat(
                (dataCap.allValues[1] as InterfaceConfiguration).getAugmentation(MdrvInterfaceConfig::class.java),
                CoreMatchers.equalTo(expectedConfigMacAddr) as Matcher<in DataObject>
        )
    }
}