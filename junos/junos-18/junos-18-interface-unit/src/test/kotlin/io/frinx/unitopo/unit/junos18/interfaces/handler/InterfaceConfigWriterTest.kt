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

package io.frinx.unitopo.unit.junos18.interfaces.handler

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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.enable.disable.Case1Builder
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceBuilder as JunosInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey

class InterfaceConfigWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: InterfaceConfigWriter

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")

        private val IF_NAME = "ae2220"
        private val IF_ENABLED = false
        private val IF_TYPE = Ieee8023adLag::class.java

        private val IID_CONFIG = IIDs.INTERFACES
            .child(Interface::class.java, InterfaceKey(IF_NAME))
            .child(Config::class.java)

        private val CONFIG = ConfigBuilder()
            .setName(IF_NAME)
            .setEnabled(IF_ENABLED)
            .setType(IF_TYPE)
            .build()

        private val NATIVE_IID = InterfaceReader.JUNOS_IFCS
            .child(JunosInterface::class.java, JunosInterfaceKey(IF_NAME))

        private val NATIVE_CONFIG = JunosInterfaceBuilder()
            .setName(IF_NAME)
            .setEnableDisable(Case1Builder().setDisable(!IF_ENABLED).build())
            .build()
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = InterfaceConfigWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG) // not customize
            .build()
        val expectedConfig = JunosInterfaceBuilder(NATIVE_CONFIG) // not customize
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<JunosInterface>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<JunosInterface>

        Mockito.doNothing().`when`(underlayAccess).safePut(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(id, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).safePut(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<JunosInterface>>
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(expectedConfig)
        )
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG) // not customize
            .build()
        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<JunosInterface>>

        Mockito.doNothing().`when`(underlayAccess).delete(Mockito.any())

        // test
        target.deleteCurrentAttributes(id, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).delete(idCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<JunosInterface>>
        )
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val configBefore = ConfigBuilder(CONFIG) // not customize
            .build()
        val configAfter = ConfigBuilder(CONFIG)
            .setEnabled(!CONFIG.isEnabled)
            .build()
        val expectedConfig = JunosInterfaceBuilder(NATIVE_CONFIG)
            .setEnableDisable(Case1Builder().setDisable(null).build())
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<JunosInterface>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<JunosInterface>

        Mockito.doNothing().`when`(underlayAccess).safeMerge(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())

        // test
        target.updateCurrentAttributes(IID_CONFIG, configBefore, configAfter, writeContext)

        // capture
        Mockito.verify(underlayAccess,
            Mockito.times(1)).safeMerge(
            Mockito.any(), Mockito.any(), idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<JunosInterface>>
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(expectedConfig)
        )
    }

    @Test
    fun testIsIfaceNameAndTypeValid() {
        // valid
        Assert.assertThat(target.isIfaceNameAndTypeValid("em0/0/0", EthernetCsmacd::class.java),
            CoreMatchers.`is`(true))
        Assert.assertThat(target.isIfaceNameAndTypeValid("et0/0/0", EthernetCsmacd::class.java),
            CoreMatchers.`is`(true))
        Assert.assertThat(target.isIfaceNameAndTypeValid("fe0/0/0", EthernetCsmacd::class.java),
            CoreMatchers.`is`(true))
        Assert.assertThat(target.isIfaceNameAndTypeValid("fxp0/0/0", EthernetCsmacd::class.java),
            CoreMatchers.`is`(true))
        Assert.assertThat(target.isIfaceNameAndTypeValid("ge0/0/0", EthernetCsmacd::class.java),
            CoreMatchers.`is`(true))
        Assert.assertThat(target.isIfaceNameAndTypeValid("xe0/0/0", EthernetCsmacd::class.java),
            CoreMatchers.`is`(true))
        Assert.assertThat(target.isIfaceNameAndTypeValid("lo0", SoftwareLoopback::class.java),
            CoreMatchers.`is`(true))
        Assert.assertThat(target.isIfaceNameAndTypeValid("ae100", Ieee8023adLag::class.java),
            CoreMatchers.`is`(true))
        Assert.assertThat(target.isIfaceNameAndTypeValid("ms0/0/0", Other::class.java),
            CoreMatchers.`is`(true))

        // invalid
        Assert.assertThat(target.isIfaceNameAndTypeValid("em0/0/0", SoftwareLoopback::class.java),
            CoreMatchers.`is`(false))
        Assert.assertThat(target.isIfaceNameAndTypeValid("ms0/0/0", EthernetCsmacd::class.java),
            CoreMatchers.`is`(false))
    }
}