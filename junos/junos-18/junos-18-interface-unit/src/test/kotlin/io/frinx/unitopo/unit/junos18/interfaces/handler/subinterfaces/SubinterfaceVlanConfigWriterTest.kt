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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.Subinterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.Subinterface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.VlanLogicalConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.Vlan
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.vlan.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.vlan.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.types.rev170714.QinqId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.types.rev170714.VlanId
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.vlan_choice.case_6.VlanTagsBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.vlan_choice.Case1Builder as JunosVlanChoiceCase1Builder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.vlan_choice.Case6Builder as JunosVlanChoiceCase6Builder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.vlan_choice.case_6.vlan.tags.inner_choice.Case1Builder as InnterChoiceCase1Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitBuilder as JunosInterfaceUnitBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitKey as JunosInterfaceUnitKey

class SubinterfaceVlanConfigWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: SubinterfaceVlanConfigWriter

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")

        private val IF_NAME = "ae2220"
        private val SUBIF_INDEX = 0L
        private val VLAN_ID = 1000
        private val OUTER_TAG = "3010"
        private val INNER_TAG = "3020"

        private val VLAN_CHOICE_NULL = JunosVlanChoiceCase1Builder().setVlanId(null).build()
        private val VLAN_CHOICE_VLANID = JunosVlanChoiceCase1Builder().setVlanId(VLAN_ID.toString()).build()
        private val VLAN_CHOICE_TAG = JunosVlanChoiceCase6Builder().setVlanTags(
            VlanTagsBuilder()
                .setOuter("0x8100:$OUTER_TAG")
                .setInnerChoice(InnterChoiceCase1Builder().setInner("0x8100:$INNER_TAG").build())
                .build())
            .build()

        private val IID_CONFIG = IIDs.INTERFACES
            .child(Interface::class.java, InterfaceKey(IF_NAME))
            .child(Subinterfaces::class.java)
            .child(Subinterface::class.java, SubinterfaceKey(SUBIF_INDEX))
            .augmentation(Subinterface1::class.java)
            .child(Vlan::class.java)
            .child(Config::class.java)

        private val CONFIG_VLANID = ConfigBuilder()
            .setVlanId(VlanLogicalConfig.VlanId(VlanId(VLAN_ID)))
            .build()

        private val CONFIG_VLAN_TAG = ConfigBuilder()
            .setVlanId(VlanLogicalConfig.VlanId(QinqId("$OUTER_TAG.$INNER_TAG")))
            .build()

        private val NATIVE_IID = InterfaceReader.JUNOS_IFCS
            .child(JunosInterface::class.java, JunosInterfaceKey(IF_NAME))
            .child(JunosInterfaceUnit::class.java, JunosInterfaceUnitKey(SUBIF_INDEX.toString()))

        private val DATA_JUNOS_IFC = NC_HELPER.read(NATIVE_IID).checkedGet().get()

        private val NATIVE_CONFIG = JunosInterfaceUnitBuilder(DATA_JUNOS_IFC)
            .setName(SUBIF_INDEX.toString())
            .setVlanChoice(VLAN_CHOICE_VLANID)
            .build()
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = SubinterfaceVlanConfigWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG_VLANID)
            .build()
        val expectedConfig = JunosInterfaceUnitBuilder() // empty
            .setVlanChoice(VLAN_CHOICE_VLANID)
            .setName(SUBIF_INDEX.toString())
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<JunosInterfaceUnit>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<JunosInterfaceUnit>

        Mockito.doNothing().`when`(underlayAccess).merge(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(id, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).merge(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<JunosInterfaceUnit>>
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(expectedConfig)
        )
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG_VLANID)
            .build()
        val expectedConfig = JunosInterfaceUnitBuilder(NATIVE_CONFIG)
            .setVlanChoice(VLAN_CHOICE_NULL)
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<JunosInterfaceUnit>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<JunosInterfaceUnit>

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.deleteCurrentAttributes(id, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<JunosInterfaceUnit>>
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(expectedConfig)
        )
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val configBefore = ConfigBuilder(CONFIG_VLANID)
            .build()
        val configAfter = ConfigBuilder(CONFIG_VLAN_TAG)
            .build()
        val expectedConfig = JunosInterfaceUnitBuilder() // empty
            .setVlanChoice(VLAN_CHOICE_TAG)
            .setName(SUBIF_INDEX.toString())
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<JunosInterfaceUnit>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<JunosInterfaceUnit>

        Mockito.doNothing().`when`(underlayAccess).merge(Mockito.any(), Mockito.any())
        // test
        target.updateCurrentAttributes(IID_CONFIG, configBefore, configAfter, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).merge(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<JunosInterfaceUnit>>
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(expectedConfig)
        )
    }
}