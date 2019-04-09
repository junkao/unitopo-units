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

package io.frinx.unitopo.unit.xr623.interfaces.handler.aggregate

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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.Bundle
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.BundleBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216.InterfaceConfiguration2 as MinLinksInterfaceConfig
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216.InterfaceConfiguration2Builder as MinLinksInterfaceConfigBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.bundle.MinimumActiveBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Interface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.Aggregation
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
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
        private val MIN_LINKS = 1

        private val NATIVE_IF_NAME = InterfaceName(IF_NAME)

        private val IID_CONFIG = IIDs.INTERFACES
                .child(Interface::class.java, InterfaceKey(IF_NAME))
                .augmentation(Interface1::class.java)
                .child(Aggregation::class.java)
                .child(Config::class.java)

        private val CONFIG = ConfigBuilder()
                .setMinLinks(MIN_LINKS)
                .build()

        private val NATIVE_IID = InterfaceReader.IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))

        private val DATA_IFC = NC_HELPER.read(NATIVE_IID).checkedGet().get()

        private val NATIVE_IID_BUNDLE = InterfaceReader.IFC_CFGS
                .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))
                .augmentation(MinLinksInterfaceConfig::class.java)
                .child(Bundle::class.java)
        private val NATIVE_CONFIG_MIN_LINKS = MinLinksInterfaceConfigBuilder(DATA_IFC
                .getAugmentation(MinLinksInterfaceConfig::class.java))
                .setBundle(BundleBuilder()
                        .setMinimumActive(MinimumActiveBuilder()
                                .setLinks(MIN_LINKS.toLong())
                                .build())
                        .build())
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

        val expectedConfigMinLinks = NATIVE_CONFIG_MIN_LINKS

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<DataObject>

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(id, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1))
                .put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        // minlinks
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID_BUNDLE) as Matcher<in InstanceIdentifier<DataObject>>
        )

        Assert.assertEquals(MIN_LINKS, (dataCap.allValues[0] as Bundle).minimumActive.links.toInt())
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
        Mockito.verify(underlayAccess, Mockito.times(1))
                .put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        // minlinks
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID_BUNDLE) as Matcher<in InstanceIdentifier<DataObject>>
        )

        Assert.assertNull((dataCap.allValues[0] as Bundle).minimumActive.links)
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val id = IID_CONFIG
        val configBefore = CONFIG
        val MIN_LINKS_AFT = 2

        val configAfter = ConfigBuilder()
                .setMinLinks(MIN_LINKS_AFT)
                .build()

        val expectedConfigMinLinks = MinLinksInterfaceConfigBuilder(DATA_IFC
                .getAugmentation(MinLinksInterfaceConfig::class.java))
                .setBundle(BundleBuilder()
                        .setMinimumActive(MinimumActiveBuilder()
                                .setLinks(MIN_LINKS_AFT.toLong())
                                .build())
                        .build())
                .build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<DataObject>

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.updateCurrentAttributes(id, configBefore, configAfter, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1))
                .put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        // minlinks
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID_BUNDLE) as Matcher<in InstanceIdentifier<DataObject>>
        )

        Assert.assertEquals(MIN_LINKS_AFT, (dataCap.allValues[0] as Bundle).minimumActive.links.toInt())
    }
}