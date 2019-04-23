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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.Bfd
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Interface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.Aggregation
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.AggregationBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.IfLagBfdAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.IfLagBfdAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.bfd.top.BfdBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.bfd.top.BfdIpv6Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.bfd.top.bfd.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.bfd.top.bfd.ipv6.ConfigBuilder as Ipv6ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class AggregateWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: AggregateWriter

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        private val NATIVE_ACT = InterfaceActive("act")
        private val IF_NAME = "Bundle-Ether3000"
        private val NATIVE_IF_NAME = InterfaceName(IF_NAME)

        private val IID_AGGREGATION = IIDs.INTERFACES
                .child(Interface::class.java, InterfaceKey(IF_NAME))
                .augmentation(Interface1::class.java)
                .child(Aggregation::class.java)
        private val NATIVE_IID_BFD = InterfaceReader.IFC_CFGS
                .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))
                .augmentation(InterfaceConfiguration1::class.java)
                .child(Bfd::class.java)
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = AggregateWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val id = IID_AGGREGATION
        val data = AggregationBuilder().addAugmentation(
                IfLagBfdAug::class.java,
                IfLagBfdAugBuilder().apply {
                    bfd = BfdBuilder().apply {
                        config = ConfigBuilder().apply {
                            destinationAddress = Ipv4Address("10.2.2.1")
                            minInterval = 1000L
                        }.build()
                    }.build()
                    bfdIpv6 = BfdIpv6Builder().apply {
                        config = Ipv6ConfigBuilder().apply {
                            destinationAddress = Ipv6Address("2001::2")
                            minInterval = 900L
                        }.build()
                    }.build()
                }.build()
        ).build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<DataObject>

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(id, data, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1))
                .put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID_BFD) as Matcher<in InstanceIdentifier<DataObject>>
        )

        val ipv4 = (dataCap.allValues[0] as Bfd).addressFamily.ipv4
        val ipv6 = (dataCap.allValues[0] as Bfd).addressFamily.ipv6
        Assert.assertEquals("10.2.2.1", ipv4.destinationAddress.value)
        Assert.assertEquals(1000L, ipv4.interval)
        Assert.assertEquals("2001::2", ipv6.ipv6DestinationAddress)
        Assert.assertEquals(900L, ipv6.ipv6Interval)
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val id = IID_AGGREGATION
        val data = AggregationBuilder().build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>

        Mockito.doNothing().`when`(underlayAccess).delete(Mockito.any())

        // test
        target.deleteCurrentAttributes(id, data, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1))
                .delete(idCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID_BFD) as Matcher<in InstanceIdentifier<DataObject>>
        )
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val id = IID_AGGREGATION
        val dataBefore = AggregationBuilder().build()
        val dataAfter = AggregationBuilder().addAugmentation(
                IfLagBfdAug::class.java,
                IfLagBfdAugBuilder().apply {
                    bfd = BfdBuilder().apply {
                        config = ConfigBuilder().apply {
                            destinationAddress = Ipv4Address("10.1.1.1")
                            minInterval = 800L
                        }.build()
                    }.build()
                    bfdIpv6 = BfdIpv6Builder().apply {
                        config = Ipv6ConfigBuilder().apply {
                            destinationAddress = Ipv6Address("2001::1")
                            minInterval = 600L
                        }.build()
                    }.build()
                }.build()
        ).build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<DataObject>

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.updateCurrentAttributes(id, dataBefore, dataAfter, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1))
                .put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID_BFD) as Matcher<in InstanceIdentifier<DataObject>>
        )

        val ipv4 = (dataCap.allValues[0] as Bfd).addressFamily.ipv4
        val ipv6 = (dataCap.allValues[0] as Bfd).addressFamily.ipv6
        Assert.assertEquals("10.1.1.1", ipv4.destinationAddress.value)
        Assert.assertEquals(800L, ipv4.interval)
        Assert.assertEquals("2001::1", ipv6.ipv6DestinationAddress)
        Assert.assertEquals(600L, ipv6.ipv6Interval)
    }
}