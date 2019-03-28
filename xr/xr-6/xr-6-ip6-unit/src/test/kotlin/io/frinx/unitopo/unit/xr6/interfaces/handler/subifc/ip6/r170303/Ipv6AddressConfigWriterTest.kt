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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r170303

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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.Ipv6Network
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.ipv6.network.Addresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.ipv6.network.addresses.LinkLocalAddress
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface2
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.Ipv6
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.address.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.address.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.Subinterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier

class Ipv6AddressConfigWriterTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: Ipv6AddressConfigWriter

    companion object {
        private val subIfcName = "Bundle-Ether202"
        private val index: Long = 0
        private val interfaceActive = InterfaceActive("act")

        private val CONFIG = ConfigBuilder().build()

        private val NC_HELPER = NetconfAccessHelper("/node.xml")
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NC_HELPER)
        target = Mockito.spy(Ipv6AddressConfigWriter(underlayAccess))
    }

    @Test
    fun testWriteCurrentAttributesLinkLocalAddress() {
        val address = "fe80:2021:41:1fff::4"
        val IID_CONFIG = InstanceIdentifier
            .create(Interfaces::class.java)
            .child(Interface::class.java, InterfaceKey(subIfcName))
            .child(Subinterfaces::class.java)
            .child(Subinterface::class.java, SubinterfaceKey(index))
            .augmentation(Subinterface2::class.java)
            .child(Ipv6::class.java)
            .child(org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip
                .rev161222.ipv6.top.ipv6.Addresses::class.java)
            .child(Address::class.java, AddressKey(Ipv6AddressNoZone(address)))
            .child(Config::class.java)
        val NATIVE_IID = KeyedInstanceIdentifier
            .create(InterfaceConfigurations::class.java)
            .child(InterfaceConfiguration::class.java,
                InterfaceConfigurationKey(interfaceActive, InterfaceName(subIfcName)))

        val config = ConfigBuilder(CONFIG).setIp(Ipv6AddressNoZone(address)) // not customize
            .build()
        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<InterfaceConfiguration>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<InterfaceConfiguration>

        Mockito.doNothing().`when`(underlayAccess).merge(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(IID_CONFIG, config, writeContext)

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
    }

    @Test
    fun testWriteCurrentAttributesRegularAddress() {
        val address = "2001:2020:42:1fff::2"
        val prefixLength: Short = 128
        val IID_CONFIG = InstanceIdentifier
            .create(Interfaces::class.java)
            .child(Interface::class.java, InterfaceKey(subIfcName))
            .child(Subinterfaces::class.java)
            .child(Subinterface::class.java, SubinterfaceKey(index))
            .augmentation(Subinterface2::class.java)
            .child(Ipv6::class.java)
            .child(org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip
                .rev161222.ipv6.top.ipv6.Addresses::class.java)
            .child(Address::class.java, AddressKey(Ipv6AddressNoZone(address)))
            .child(Config::class.java)
        val NATIVE_IID = KeyedInstanceIdentifier
            .create(InterfaceConfigurations::class.java)
            .child(InterfaceConfiguration::class.java,
                InterfaceConfigurationKey(interfaceActive, InterfaceName(subIfcName)))

        val config = ConfigBuilder(CONFIG)
            .setIp(Ipv6AddressNoZone(address))
            .setPrefixLength(prefixLength) // not customize
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<InterfaceConfiguration>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<InterfaceConfiguration>

        Mockito.doNothing().`when`(underlayAccess).merge(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(IID_CONFIG, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1))
            .merge(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<InterfaceConfiguration>>
        )
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val address = "fe80:2021:41:1fff::4"
        val IID_CONFIG = InstanceIdentifier
            .create(Interfaces::class.java)
            .child(Interface::class.java, InterfaceKey(subIfcName))
            .child(Subinterfaces::class.java)
            .child(Subinterface::class.java, SubinterfaceKey(index))
            .augmentation(Subinterface2::class.java)
            .child(Ipv6::class.java)
            .child(org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip
                .rev161222.ipv6.top.ipv6.Addresses::class.java)
            .child(Address::class.java, AddressKey(Ipv6AddressNoZone(address)))
            .child(Config::class.java)
        val NATIVE_IID = KeyedInstanceIdentifier
            .create(InterfaceConfigurations::class.java)
            .child(InterfaceConfiguration::class.java,
                InterfaceConfigurationKey(interfaceActive, InterfaceName(subIfcName)))
            .augmentation(InterfaceConfiguration1::class.java)
            .child(Ipv6Network::class.java)
            .child(Addresses::class.java)
            .child(LinkLocalAddress::class.java)
        val config = ConfigBuilder(CONFIG)
            .setIp(Ipv6AddressNoZone(address))
            .setPrefixLength(64) // not customize
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<LinkLocalAddress>>

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
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<LinkLocalAddress>>
        )
    }
}