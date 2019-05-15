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

package io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceReader
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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.extention.rev181204.Config1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.extention.rev181204.Config1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.extention.rev181204.RpmTypes
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.Subinterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.ConfigBuilder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitBuilder as JunosInterfaceUnitBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitKey as JunosInterfaceUnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.enable.disable.Case1Builder as JunosCase1Builder

class SubinterfaceConfigWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: SubinterfaceConfigWriter

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")

        private val UNIT_IS_ENABLE = JunosCase1Builder().setDisable(null).build()
        private val UNIT_IS_DISABLE = JunosCase1Builder().setDisable(true).build()

        private val IF_NAME = "ae2220"
        private val SUBIF_INDEX = 0L
        private val IF_ENABLED = false
        private val DESCRIPTION = "ae2220-description-update"

        private val IID_CONFIG = IIDs.INTERFACES
            .child(Interface::class.java, InterfaceKey(IF_NAME))
            .child(Subinterfaces::class.java)
            .child(Subinterface::class.java, SubinterfaceKey(SUBIF_INDEX))
            .child(Config::class.java)

        private val CONFIG = ConfigBuilder()
            .setIndex(SUBIF_INDEX)
            .setEnabled(IF_ENABLED)
            .setDescription(DESCRIPTION)
            .build()
        private val CONFIG1 = Config1Builder()
                .setRpmType(RpmTypes.ClientDelegateProbes).build()

        private val NATIVE_IID = InterfaceReader.JUNOS_IFCS
            .child(JunosInterface::class.java, JunosInterfaceKey(IF_NAME))
            .child(JunosInterfaceUnit::class.java, JunosInterfaceUnitKey(SUBIF_INDEX.toString()))

        private val DATA_JUNOS_IFC = NC_HELPER.read(NATIVE_IID).checkedGet().get()

        private val NATIVE_CONFIG = JunosInterfaceUnitBuilder(DATA_JUNOS_IFC)
            .setName(SUBIF_INDEX.toString())
            .setEnableDisable(UNIT_IS_ENABLE)
            .setDescription(DESCRIPTION)
            .build()
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = SubinterfaceConfigWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes_Enabled() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG).apply {
            this.addAugmentation(Config1::class.java, Config1Builder().apply {
                this.rpmType = RpmTypes.ClientDelegateProbes
            }.build())
        }.setEnabled(true).build()

        val expectedConfig = JunosInterfaceUnitBuilder(NATIVE_CONFIG)
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<JunosInterfaceUnit>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<JunosInterfaceUnit>

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
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<JunosInterfaceUnit>>
        )
    }

    @Test
    fun testWriteCurrentAttributes_Null() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG).apply {
            this.addAugmentation(Config1::class.java, Config1Builder().apply {
                this.rpmType = RpmTypes.ClientDelegateProbes
            }.build())
        }.setEnabled(null).build()
        val expectedConfig = JunosInterfaceUnitBuilder(NATIVE_CONFIG)
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<JunosInterfaceUnit>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<JunosInterfaceUnit>

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
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<JunosInterfaceUnit>>
        )
    }

    @Test
    fun testWriteCurrentAttributes_Disable() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG).apply {
            this.addAugmentation(Config1::class.java, Config1Builder().apply {
                this.rpmType = RpmTypes.ClientDelegateProbes
            }.build())
        }.setEnabled(false).build()
        val expectedConfig = JunosInterfaceUnitBuilder(NATIVE_CONFIG)
            .setEnableDisable(UNIT_IS_DISABLE)
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<JunosInterfaceUnit>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<JunosInterfaceUnit>

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
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<JunosInterfaceUnit>>
        )
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG).apply {
            this.addAugmentation(Config1::class.java, Config1Builder().apply {
                this.rpmType = RpmTypes.ClientDelegateProbes
            }.build())
        }.build()
        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<JunosInterfaceUnit>>

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
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<JunosInterfaceUnit>>
        )
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val NEW_DESCRIPTION = "$DESCRIPTION - update"
        val configBefore = ConfigBuilder(CONFIG)
            .build()

        val configAfter = ConfigBuilder(CONFIG).apply {
            this.addAugmentation(Config1::class.java, Config1Builder().apply {
                this.rpmType = RpmTypes.ClientDelegateProbes
            }.build())
        }.build()

            val expectedConfig = JunosInterfaceUnitBuilder(NATIVE_CONFIG)
            .setEnableDisable(JunosCase1Builder().setDisable(null).build())
            .setDescription(NEW_DESCRIPTION)
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<JunosInterfaceUnit>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<JunosInterfaceUnit>

        Mockito.doNothing().`when`(underlayAccess).safeMerge(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())

        // test
        target.updateCurrentAttributes(IID_CONFIG, configBefore, configAfter, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).safeMerge(Mockito.any(),
            Mockito.any(), idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<JunosInterfaceUnit>>
        )
    }
}