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

package io.frinx.unitopo.unit.xr7.interfaces.handler.ethernet

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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501.BundlePortActivity
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501.InterfaceConfiguration3
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501.InterfaceConfiguration4
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501.PeriodShortEnum
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501._interface.configurations._interface.configuration.BundleMember
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501._interface.configurations._interface.configuration.Lacp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501._interface.configurations._interface.configuration.LacpBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501._interface.configurations._interface.configuration.bundle.member.Id
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev170501._interface.configurations._interface.configuration.bundle.member.IdBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Config1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Config1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.Interface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.Ethernet
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.ethernet.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.ethernet.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.lag.member.rev171109.LacpEthConfigAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.lag.member.rev171109.LacpEthConfigAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.LacpActivityType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.LacpPeriodType
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class EthernetConfigWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: EthernetConfigWriter

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        private val NATIVE_ACT = InterfaceActive("act")
        private val IF_NAME = "TenGigE0/0/0/2"
        val ETH_BUNDLEID = "Bundle-Ether65535"
        val ETH_LACPMODE = LacpActivityType.PASSIVE
        val ETH_LACPPERIODTYPE = LacpPeriodType.FAST

        private val NATIVE_IF_NAME = InterfaceName(IF_NAME)
        private val NATIVE_BUNDLEID = 65535L
        private val NATIVE_PORTACTIVITY = BundlePortActivity.Passive

        private val IID_CONFIG = IIDs.INTERFACES
                .child(Interface::class.java, InterfaceKey(IF_NAME))
                .augmentation(Interface1::class.java)
                .child(Ethernet::class.java)
                .child(Config::class.java)

        private val aggregateBuilder = Config1Builder()
                .setAggregateId(ETH_BUNDLEID)

        private val lacpEthConfigBuilder = LacpEthConfigAugBuilder()
                .setLacpMode(ETH_LACPMODE)
                .setInterval(ETH_LACPPERIODTYPE)

        private val CONFIG = ConfigBuilder()
                .addAugmentation(Config1::class.java, aggregateBuilder.build())
                .addAugmentation(LacpEthConfigAug::class.java, lacpEthConfigBuilder.build())
            .build()

        private val NATIVE_IID = InterfaceReader.IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))

        private val DATA_ETHER_IFC = NC_HELPER.read(NATIVE_IID).checkedGet().get()

        private val NATIVE_IID_BUNDLE = InterfaceReader.IFC_CFGS
                .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))
                .augmentation(InterfaceConfiguration4::class.java).child(BundleMember::class.java)
                .child(Id::class.java)

        private val NATIVE_CONFIG_BUNDLE = IdBuilder(DATA_ETHER_IFC
                .getAugmentation(InterfaceConfiguration4::class.java).bundleMember.id)
                .setBundleId(NATIVE_BUNDLEID)
                .setPortActivity(NATIVE_PORTACTIVITY)
                .build()

        private val NATIVE_IID_LACP = InterfaceReader.IFC_CFGS.child(InterfaceConfiguration::class.java,
                InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))
                .augmentation(InterfaceConfiguration3::class.java).child(Lacp::class.java)

        private val NATIVE_CONFIG_LACP = LacpBuilder(DATA_ETHER_IFC
                .getAugmentation(InterfaceConfiguration3::class.java).lacp)
                .setPeriodShort(PeriodShortEnum(PeriodShortEnum.Enumeration.True))
                .build()
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = EthernetConfigWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG) // not customize
                .build()

        val expectedConfigBundle = IdBuilder(NATIVE_CONFIG_BUNDLE) // not customize
                .build()

        val expectedConfigLacp = LacpBuilder(NATIVE_CONFIG_LACP) // not customize
                .build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<DataObject>

        Mockito.doNothing().`when`(underlayAccess).merge(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(id, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(2))
                .merge(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(2))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(2))

        // verify captured values
        // bundle
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID_BUNDLE) as Matcher<in InstanceIdentifier<DataObject>>
        )
        Assert.assertThat(
                dataCap.allValues[0],
                CoreMatchers.equalTo(expectedConfigBundle) as Matcher<in DataObject>
        )

        // lacp
        Assert.assertThat(
                idCap.allValues[1],
                CoreMatchers.equalTo(NATIVE_IID_LACP) as Matcher<in InstanceIdentifier<DataObject>>
        )
        Assert.assertThat(
                dataCap.allValues[1],
                CoreMatchers.equalTo(expectedConfigLacp) as Matcher<in DataObject>
        )
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val NATIVE_IID_BUNDLE_DELETE = InterfaceReader.IFC_CFGS
                .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))
                .augmentation(InterfaceConfiguration4::class.java)
                .child(BundleMember::class.java)

        val NATIVE_IID_LACP_DELETE = InterfaceReader.IFC_CFGS.child(InterfaceConfiguration::class.java,
                InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))
                .augmentation(InterfaceConfiguration3::class.java)
                .child(Lacp::class.java)

        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG) // not customize
            .build()
        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>

        Mockito.doNothing().`when`(underlayAccess).delete(Mockito.any())

        // test
        target.deleteCurrentAttributes(id, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(2)).delete(idCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(2))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID_BUNDLE_DELETE) as Matcher<in InstanceIdentifier<DataObject>>
        )
        Assert.assertThat(
                idCap.allValues[1],
                CoreMatchers.equalTo(NATIVE_IID_LACP_DELETE) as Matcher<in InstanceIdentifier<DataObject>>
        )
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val ETH_LACPMODE_AFTER = LacpActivityType.ACTIVE
        val NATIVE_PORTACTIVITY_AFTER = BundlePortActivity.Active
        val NATIVE_CONFIG_BUNDLE_AFTER = IdBuilder(DATA_ETHER_IFC
                .getAugmentation(InterfaceConfiguration4::class.java).bundleMember.id)
                .setBundleId(NATIVE_BUNDLEID)
                .setPortActivity(NATIVE_PORTACTIVITY_AFTER)
                .build()

        val lacpEthConfigBuilderAfter = lacpEthConfigBuilder.setLacpMode(ETH_LACPMODE_AFTER)

        val CONFIG_AFTER = ConfigBuilder()
                .addAugmentation(Config1::class.java, aggregateBuilder.build())
                .addAugmentation(LacpEthConfigAug::class.java, lacpEthConfigBuilderAfter.build())
                .build()

        val configBefore = ConfigBuilder(CONFIG) // not customize
            .build()
        val configAfter = ConfigBuilder(CONFIG_AFTER).build()

        val expectedConfigBundle = IdBuilder(NATIVE_CONFIG_BUNDLE_AFTER) // not customize
                .build()

        val expectedConfigLacp = LacpBuilder(NATIVE_CONFIG_LACP) // not customize
                .build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<DataObject>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<DataObject>

        Mockito.doNothing().`when`(underlayAccess).merge(Mockito.any(), Mockito.any())

        // test
        target.updateCurrentAttributes(IID_CONFIG, configBefore, configAfter, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(2))
                .merge(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(2))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(2))

        // verify captured values
        // bundle
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID_BUNDLE) as Matcher<in InstanceIdentifier<DataObject>>
        )
        Assert.assertThat(
                dataCap.allValues[0],
                CoreMatchers.equalTo(expectedConfigBundle) as Matcher<in DataObject>
        )

        // lacp
        Assert.assertThat(
                idCap.allValues[1],
                CoreMatchers.equalTo(NATIVE_IID_LACP) as Matcher<in InstanceIdentifier<DataObject>>
        )
        Assert.assertThat(
                dataCap.allValues[1],
                CoreMatchers.equalTo(expectedConfigLacp) as Matcher<in DataObject>
        )
    }
}