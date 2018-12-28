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

package io.frinx.unitopo.unit.junos18.probes.handler.test

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.probes.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.probes.handler.ProbeReader
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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.Probe
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.ProbeKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.test.target.target.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.test.target.target.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.common.types.rev180101.Ipv4addr
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.test.target.target.type.Case1Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.Test as ProbeTest
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probe.tests.top.TestKey as ProbeTestKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.probe.Tests as ProbeTests
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.test.target.Target as ProbeTarget
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.Rpm as JunosRpm
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.Probe as JunosProbe
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.ProbeKey as JunosProbeKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.Test as JunosTest
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.TestKey as JunosTestKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.test.Target as JunosTarget
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.services.rpm.probe.test.TargetBuilder as JunosTargetBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class ProbeTargetConfigWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: ProbeTargetConfigWriter

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")

        private val PROBE_NAME = "MS00"
        private val PROBE_TEST_NAME = "ae2220.46_SBM-P-00000827-19"
        private val SOURCE_IP = "10.128.178.94"

        private val IID_CONFIG = IIDs.PROBES
                .child(Probe::class.java, ProbeKey(PROBE_NAME))
                .child(ProbeTests::class.java)
                .child(ProbeTest::class.java, ProbeTestKey(PROBE_TEST_NAME))
                .child(ProbeTarget::class.java)
                .child(Config::class.java)

        private val CONFIG = ConfigBuilder()
                .setAddress(IpAddress(SOURCE_IP.toCharArray()))
                .build()

        private val NATIVE_IID = ProbeReader.JUNOS_SERVICES
                .child(JunosRpm::class.java)
                .child(JunosProbe::class.java, JunosProbeKey(PROBE_NAME))
                .child(JunosTest::class.java, JunosTestKey(PROBE_TEST_NAME))
                .child(JunosTarget::class.java)

        private val NATIVE_EXISTING_CONFIG = NC_HELPER.read(NATIVE_IID).checkedGet().get()

        private val NATIVE_CREATED_CONFIG = JunosTargetBuilder()
                .setTargetType(Case1Builder().setAddress(Ipv4addr(SOURCE_IP)).build())
                .build()

        private val SOURCE_IP_UPDATE = "99.128.178.94"
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = ProbeTargetConfigWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG)
                .build()
        val expectedConfig = JunosTargetBuilder(NATIVE_CREATED_CONFIG)
                .build()

        val idCap = ArgumentCaptor
                .forClass(IID::class.java) as ArgumentCaptor<IID<JunosTarget>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<JunosTarget>

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(id, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<JunosTarget>>
        )
        Assert.assertThat(
                dataCap.allValues[0],
                CoreMatchers.equalTo(expectedConfig)
        )
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG)
                .build()
        val idCap = ArgumentCaptor
                .forClass(IID::class.java) as ArgumentCaptor<IID<JunosTarget>>

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
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<JunosTarget>>
        )
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val configBefore = ConfigBuilder(CONFIG)
                .build()
        val configAfter = ConfigBuilder(CONFIG)
                .setAddress(IpAddress(SOURCE_IP_UPDATE.toCharArray()))
                .build()
        val expectedConfig = JunosTargetBuilder(NATIVE_EXISTING_CONFIG)
                .setTargetType(Case1Builder().setAddress(Ipv4addr(SOURCE_IP_UPDATE)).build())
                .build()

        val idCap = ArgumentCaptor
                .forClass(IID::class.java) as ArgumentCaptor<IID<JunosTarget>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<JunosTarget>

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
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<JunosTarget>>
        )
        Assert.assertThat(
                dataCap.allValues[0],
                CoreMatchers.equalTo(expectedConfig)
        )
    }
}