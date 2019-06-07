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
package io.frinx.unitopo.unit.xr66.interfaces.handler

import com.google.common.util.concurrent.CheckedFuture
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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.statsd.cfg.rev170501.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.statsd.cfg.rev170501._interface.configurations._interface.configuration.StatisticsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.Interface2
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.Statistics
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.statistics.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.statistics.top.statistics.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.statsd.cfg.rev170501._interface.configurations._interface.configuration.Statistics as ICS_Statistics

class InterfaceStatisticsConfigWriterTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: InterfaceStatisticsConfigWriter

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")

        // Open Config
        private val IF_NAME = "Bundle-Ether301"
        private val IF_LOADINTERVAL = 30

        // netconf
        private val NATIVE_IF_NAME = InterfaceName(IF_NAME)
        private val NATIVE_ACT = InterfaceActive("act")
        private val IID_CONFIG = KeyedInstanceIdentifier
                .create(Interfaces::class.java)
                .child(Interface::class.java, InterfaceKey(IF_NAME))
                .augmentation(Interface2::class.java)
                .child(Statistics::class.java).child(Config::class.java)

        private val NATIVE_IID: InstanceIdentifier<ICS_Statistics> =
                InstanceIdentifier
                        .create(InterfaceConfigurations::class.java)
                        .child(InterfaceConfiguration::class.java,
                                InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))
                        .augmentation(InterfaceConfiguration1::class.java)
                        .child(ICS_Statistics::class.java)

        private val NATIVE_CONFIG = StatisticsBuilder().setLoadInterval(IF_LOADINTERVAL.toLong()).build()
    }
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = InterfaceStatisticsConfigWriter(underlayAccess)
    }
    @Test
    fun testWriteCurrentAttributes() {
        val config = ConfigBuilder().apply {
            this.setLoadInterval(IF_LOADINTERVAL.toLong())
        }.build()
        val expectedConfig = StatisticsBuilder(NATIVE_CONFIG).build()

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
            this.setLoadInterval(IF_LOADINTERVAL.toLong())
        }.build()
        val expectedConfig = StatisticsBuilder(NATIVE_CONFIG) // not customize
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
            this.setLoadInterval(IF_LOADINTERVAL.toLong())
        }.build()
        val configAfter = ConfigBuilder().apply {
            this.setLoadInterval(90.toLong())
        }.build()
        val expectedConfig = StatisticsBuilder(NATIVE_CONFIG)
                .setLoadInterval(90.toLong())
                .build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<InterfaceConfiguration>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<InterfaceConfiguration>

        val checkedFuture = Mockito.mock(CheckedFuture::class.java)

        Mockito.doReturn(checkedFuture)
                .`when`(underlayAccess)
                .read(NATIVE_IID)

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
                CoreMatchers.equalTo(expectedConfig) as Matcher<in InterfaceConfiguration>
        )
    }
}