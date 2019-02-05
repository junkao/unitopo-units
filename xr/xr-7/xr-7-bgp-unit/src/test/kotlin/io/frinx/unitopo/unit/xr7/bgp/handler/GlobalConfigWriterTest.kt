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

package io.frinx.unitopo.unit.xr7.bgp.handler

import com.google.common.base.Optional
import com.google.common.util.concurrent.CheckedFuture
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.Bgp
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.Global
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier

class GlobalConfigWriterTest : AbstractNetconfHandlerTest() {

    @Mock
    private lateinit var underlayAccess: UnderlayAccess
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var target: GlobalConfigWriter

    companion object {
        private val IF_NAME = "default"
        private val IF_AS = AsNumber(1999)

        private val IID_CONFIG = KeyedInstanceIdentifier
                .create(NetworkInstances::class.java)
                .child(NetworkInstance::class.java, NetworkInstanceKey("default"))
                .child(Protocols::class.java)
                .child(Protocol::class.java, ProtocolKey(BGP::class.java, "default"))
                .child(org.opendaylight.yang.gen.v1
                        .http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.Bgp::class.java)
                .child(Global::class.java)
                .child(Config::class.java)

        private val CONFIG = ConfigBuilder()
                .setAs(IF_AS)
                .build()

        private val NATIVE_IID: InstanceIdentifier<Bgp> = InstanceIdentifier
                .create(Bgp::class.java)

        private val DATA_NODES = getResourceAsString(javaClass, "/bgp-conf3.xml")
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        target = Mockito.spy(GlobalConfigWriter(underlayAccess!!))
    }

    @Test
    fun testWriteCurrentAttributesForType() {
        val config = ConfigBuilder(CONFIG) // not customize
                .build()
        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<Bgp>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<Bgp>
        Mockito.doNothing().`when`(underlayAccess!!).put(Mockito.any(), Mockito.any())
        target!!.writeCurrentAttributesForType(IID_CONFIG, config, writeContext!!)
        // capture
        Mockito.verify(underlayAccess!!, Mockito.times(1)).put(idCap.capture(), dataCap.capture())
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(
                idCap.allValues.get(0),
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<Bgp>>
        )
    }

    @Test
    fun testdeleteCurrentAttributesForType() {
        val config = ConfigBuilder(CONFIG) // not customize
                .build()
        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<Bgp>>

        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<Bgp>
        Mockito.doNothing().`when`(underlayAccess!!).put(Mockito.any(), Mockito.any())
        target!!.deleteCurrentAttributesForType(IID_CONFIG, config, writeContext!!)
        // capture
        Mockito.verify(underlayAccess!!, Mockito.times(1)).delete(idCap.capture())
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(
                idCap.allValues.get(0),
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<Bgp>>
        )
    }

    @Test
    fun testUpdateCurrentAttributesForType() {
        val configBefore = ConfigBuilder(CONFIG) // not customize
                .build()
        var configAfter = ConfigBuilder(CONFIG)
                .setAs(AsNumber(2000))
                .build()

        val checkedFuture = Mockito.mock(CheckedFuture::class.java)
        val data = parseGetCfgResponse(DATA_NODES, NATIVE_IID)
        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<Bgp>>

        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<Bgp>
        Mockito.doReturn(checkedFuture)
                .`when`(underlayAccess!!)
                .read(NATIVE_IID)
        Mockito.doReturn(Optional.of(data)).`when`(checkedFuture).checkedGet()

        Mockito.doNothing().`when`(underlayAccess!!).put(Mockito.any(), Mockito.any())

        target!!.updateCurrentAttributesForType(IID_CONFIG, configBefore, configAfter, writeContext!!)
        Mockito.verify(underlayAccess!!, Mockito.times(1)).put(idCap.capture(), dataCap.capture())
        Assert.assertThat(
                idCap.allValues.get(0),
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<Bgp>>
        )
    }
}