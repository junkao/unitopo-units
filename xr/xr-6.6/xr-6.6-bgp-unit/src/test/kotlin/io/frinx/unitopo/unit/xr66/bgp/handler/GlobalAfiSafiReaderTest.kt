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

package io.frinx.unitopo.unit.xr66.bgp.handler

import com.google.common.base.Optional
import com.google.common.util.concurrent.CheckedFuture
import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.AfiSafis
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.Bgp
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.Global
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.L2VPNEVPN
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class GlobalAfiSafiReaderTest : AbstractNetconfHandlerTest() {

    @Mock
    private lateinit var underlayAccess: UnderlayAccess
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var target: GlobalAfiSafiReader

    private val DATA_NODES = getResourceAsString("/bgp-conf3.xml")

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        target = Mockito.spy(GlobalAfiSafiReader(underlayAccess!!))
    }

    companion object {

        private val IID_AFISAFI = InstanceIdentifier
                .create(NetworkInstances::class.java)
                .child(NetworkInstance::class.java, NetworkInstanceKey("default"))
                .child(Protocols::class.java)
                .child(Protocol::class.java, ProtocolKey(BGP::class.java, "default"))
                .child(Bgp::class.java)
                .child(Global::class.java)
                .child(AfiSafis::class.java)
                .child(AfiSafi::class.java, AfiSafiKey(L2VPNEVPN::class.java))

        private val IID_BGP_INSTANCE = BgpProtocolReader.UNDERLAY_BGP
                .child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))
    }

    @Test
    fun testReadCurrentAttributesForType() {
        val builder = AfiSafiBuilder()
        target!!.readCurrentAttributes(IID_AFISAFI, builder, readContext!!)
        Assert.assertEquals(L2VPNEVPN::class.java, builder.build().afiSafiName)
    }

    @Test
    fun testGetBuilder() {
        val builder = target!!.getBuilder(IID_AFISAFI)
        Assert.assertTrue(builder is AfiSafiBuilder)
    }

    @Test
    fun testGetAllIdsForType() {

        val checkedFuture = Mockito.mock(CheckedFuture::class.java)
        val data = parseGetCfgResponse(DATA_NODES, IID_BGP_INSTANCE)

        Mockito.doReturn(checkedFuture)
                .`when`(underlayAccess!!)
                .read(IID_BGP_INSTANCE)
        Mockito.doReturn(Optional.of(data)).`when`(checkedFuture).checkedGet()

        val list = target!!.getAllIds(IID_AFISAFI, readContext!!)
        Assert.assertTrue(
                list.map { it.afiSafiName }.contains(L2VPNEVPN::class.java))
    }
}