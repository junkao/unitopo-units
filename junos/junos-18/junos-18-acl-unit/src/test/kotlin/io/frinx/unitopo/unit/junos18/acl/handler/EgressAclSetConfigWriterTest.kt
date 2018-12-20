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

package io.frinx.unitopo.unit.junos18.acl.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.acl.IIDs
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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.ACLIPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.ACLIPV6
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.EgressAclSets
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.egress.acl.sets.EgressAclSet
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.egress.acl.sets.EgressAclSetKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.egress.acl.sets.egress.acl.set.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.egress.acl.sets.egress.acl.set.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.Family
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.Inet
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.Filter
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.filter.output_choice.case_1.Output
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.filter.output_choice.case_1.OutputBuilder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitKey as JunosInterfaceUnitKey

class EgressAclSetConfigWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: EgressAclSetConfigWriter

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        private val IF_NAME = "ae2220"
        private val UNIT_NAME = "46"
        private val IF_ID = "$IF_NAME.$UNIT_NAME"
        private val ACL_TYPE = ACLIPV4::class.java
        private val OUTPUT_FILTER_NAME = "FBF-MS-THU-RETURN-OUTPUT"
        private val EGRESS_ACL_SET_KEY = EgressAclSetKey(OUTPUT_FILTER_NAME, ACL_TYPE)
        private val IID_CONFIG = IIDs.AC_INTERFACES
            .child(Interface::class.java, InterfaceKey(InterfaceId(IF_ID)))
            .child(EgressAclSets::class.java)
            .child(EgressAclSet::class.java, EGRESS_ACL_SET_KEY)
            .child(Config::class.java)

        private val CONFIG = ConfigBuilder()
            .setSetName(OUTPUT_FILTER_NAME)
            .setType(ACL_TYPE)
            .build()

        private val NATIVE_IID = AclInterfaceReader.JUNOS_IFCS
            .child(JunosInterface::class.java, JunosInterfaceKey(IF_NAME))
            .child(JunosInterfaceUnit::class.java, JunosInterfaceUnitKey(UNIT_NAME))
            .child(Family::class.java)
            .child(Inet::class.java)
            .child(Filter::class.java)
            .child(Output::class.java)

        private val ACL_TYPE_INET6 = ACLIPV6::class.java
        private val EGRESS_ACL_SET_KEY_INET6 = EgressAclSetKey(OUTPUT_FILTER_NAME, ACL_TYPE_INET6)
        private val IID_CONFIG_INET6 = IIDs.AC_INTERFACES
            .child(Interface::class.java, InterfaceKey(InterfaceId(IF_ID)))
            .child(EgressAclSets::class.java)
            .child(EgressAclSet::class.java, EGRESS_ACL_SET_KEY_INET6)
            .child(Config::class.java)
        private val CONFIG_INET6 = ConfigBuilder()
            .setSetName(OUTPUT_FILTER_NAME)
            .setType(ACL_TYPE_INET6)
            .build()

        private val DATA_JUNOS_IFC = NC_HELPER.read(NATIVE_IID).checkedGet().get()

        private val NATIVE_CONFIG = OutputBuilder(DATA_JUNOS_IFC).build()
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = EgressAclSetConfigWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes_Normal() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG)
            .build()
        val expectedConfig = OutputBuilder(NATIVE_CONFIG)
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<Output>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<Output>

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
            CoreMatchers.equalTo(NATIVE_IID)
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(expectedConfig)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testWriteCurrentAttributes_Inet6() {
        val id = IID_CONFIG_INET6
        val config = ConfigBuilder(CONFIG_INET6)
            .build()

        // test
        target.writeCurrentAttributes(id, config, writeContext)
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG)
            .build()
        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<Filter>>

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
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<Filter>>
        )
    }
}