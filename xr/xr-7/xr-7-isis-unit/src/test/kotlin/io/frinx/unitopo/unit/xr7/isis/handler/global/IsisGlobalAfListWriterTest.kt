/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.isis.handler.global

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import io.frinx.unitopo.unit.xr7.isis.handler.IsisProtocolReader
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.Afs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev190405.IsisAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev190405.IsisInstanceName
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev190405.IsisSubAddressFamily
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.IPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.Af
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.AfBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.AfKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.global.base.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.top.Isis
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.top.isis.Global
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.ISIS
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.Af as UlAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.AfBuilder as UlAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.AfKey as UlAfKey

class IsisGlobalAfListWriterTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var underlayAccess: UnderlayAccess

    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var target: IsisGlobalAfListWriter

    companion object {
        val IID_AF = InstanceIdentifier
            .create(NetworkInstances::class.java)
            .child(NetworkInstance::class.java, NetworkInstanceKey("default"))
            .child(Protocols::class.java)
            .child(Protocol::class.java, ProtocolKey(ISIS::class.java, "ISIS-001"))
            .child(Isis::class.java)
            .child(Global::class.java)
            .child(AfiSafi::class.java)
            .child(Af::class.java, AfKey(IPV4::class.java, UNICAST::class.java))
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper("/data_nodes.xml"))
        target = IsisGlobalAfListWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val af = AfBuilder()
            .setAfiName(IPV4::class.java)
            .setSafiName(UNICAST::class.java)
            .build()
        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<UlAf>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<UlAf>

        Mockito.doNothing().`when`(underlayAccess).safePut(Mockito.any(), Mockito.any())
        target.writeCurrentAttributes(IID_AF, af, writeContext)
        Mockito.verify(underlayAccess, Mockito.times(1))
            .safePut(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify parameter to underlay
        val expectedId = IsisProtocolReader.UNDERLAY_ISIS
            .child(Instance::class.java, InstanceKey(IsisInstanceName("ISIS-001")))
            .child(Afs::class.java)
            .child(UlAf::class.java, UlAfKey(IsisAddressFamily.Ipv4, IsisSubAddressFamily.Unicast))
        val expectedData = UlAfBuilder()
            .setAfName(IsisAddressFamily.Ipv4)
            .setSafName(IsisSubAddressFamily.Unicast)
            .build()
        Assert.assertThat(idCap.allValues.get(0),
            CoreMatchers.equalTo(expectedId) as Matcher<in InstanceIdentifier<UlAf>>)
        Assert.assertThat(dataCap.allValues.get(0),
            CoreMatchers.equalTo(expectedData) as Matcher<in UlAf>)
    }

    @Test
    fun testdeleteCurrentAttributes() {
        val dataBefore = AfBuilder() .build()
        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<UlAf>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<UlAf>
        Mockito.doNothing().`when`(underlayAccess).safeDelete(Mockito.any(), Mockito.any())
        target.deleteCurrentAttributes(IID_AF, dataBefore, writeContext)
        Mockito.verify(underlayAccess, Mockito.times(1))
            .safeDelete(idCap.capture(), dataCap.capture())
        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify parameter to underlay
        val expectedId = IsisProtocolReader.UNDERLAY_ISIS
            .child(Instance::class.java, InstanceKey(IsisInstanceName("ISIS-001")))
            .child(Afs::class.java)
            .child(UlAf::class.java, UlAfKey(IsisAddressFamily.Ipv4, IsisSubAddressFamily.Unicast))
        Assert.assertThat(idCap.allValues.get(0),
            CoreMatchers.equalTo(expectedId) as Matcher<in InstanceIdentifier<UlAf>>)
    }
}