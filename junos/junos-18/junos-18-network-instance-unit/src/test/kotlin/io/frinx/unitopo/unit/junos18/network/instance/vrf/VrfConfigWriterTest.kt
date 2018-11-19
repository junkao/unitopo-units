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

package io.frinx.unitopo.unit.junos18.network.instance.vrf

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.network.instance.IIDs
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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L3VRF
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.Instance
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.InstanceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.InstanceKey
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class VrfConfigWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: VrfConfigWriter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = VrfConfigWriter(underlayAccess)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testWriteCurrentAttributes() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG) // not customize
                .build()
        val expectedConfig = InstanceBuilder(NATIVE_CONFIG) // not customize
                .build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<Instance>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<Instance>

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
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<InstanceIdentifier<Instance>>
        )
        Assert.assertThat(
                dataCap.allValues[0],
                CoreMatchers.equalTo(expectedConfig)
        )
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testDeleteCurrentAttributes() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG) // not customize
                .build()
        val expectedConfig = InstanceBuilder() // empty
            .setName(VRF_NAME)
            .build()
        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<Instance>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<Instance>

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
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<InstanceIdentifier<Instance>>
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(expectedConfig)
        )
    }

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")

        private val VRF_NAME = "THU"
        private val INST_TYPE = L3VRF::class.java

        private val IID_CONFIG = IIDs.NETWORKINSTANCES
                .child(NetworkInstance::class.java, NetworkInstanceKey(VRF_NAME))
                .child(Config::class.java)

        private val CONFIG = ConfigBuilder()
                .setName(VRF_NAME)
                .setType(INST_TYPE)
                .build()

        private val NATIVE_IID = VrfReader.JUNOS_VRFS_ID
                .child(Instance::class.java, InstanceKey(VRF_NAME))

        private val NATIVE_CONFIG = InstanceBuilder()
                .setName(VRF_NAME)
                .build()
    }
}