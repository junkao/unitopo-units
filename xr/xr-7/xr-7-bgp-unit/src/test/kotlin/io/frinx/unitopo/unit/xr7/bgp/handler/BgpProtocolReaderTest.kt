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
package io.frinx.unitopo.unit.xr7.bgp.handler

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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class BgpProtocolReaderTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var underlayAccess: UnderlayAccess
    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var target: BgpProtocolReader

    private val DATA_NODES = getResourceAsString("/bgp-conf3.xml")

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        target = Mockito.spy(BgpProtocolReader(underlayAccess!!))
    }

    companion object {

        private val IID_BGP_PROT = InstanceIdentifier
                .create(NetworkInstances::class.java)
                .child(NetworkInstance::class.java, NetworkInstanceKey("default"))
                .child(Protocols::class.java)
                .child(Protocol::class.java, ProtocolKey(BGP::class.java, "default"))
    }

    @Test
    fun testReadCurrentAttributesForType() {
        val builder = ProtocolBuilder()
        target!!.readCurrentAttributes(IID_BGP_PROT, builder, readContext!!)
        Assert.assertEquals("default", builder.build().name)
    }

    @Test
    fun testGetAllIdsForType() {

        val checkedFuture = Mockito.mock(CheckedFuture::class.java)
        val data = parseGetCfgResponse(DATA_NODES, BgpProtocolReader.UNDERLAY_BGP)

        Mockito.doReturn(checkedFuture)
                .`when`(underlayAccess!!)
                .read(BgpProtocolReader.UNDERLAY_BGP, LogicalDatastoreType.CONFIGURATION)
        Mockito.doReturn(Optional.of(data)).`when`(checkedFuture).checkedGet()

        val list = target!!.getAllIds(IID_BGP_PROT, readContext!!)
        Assert.assertTrue(
                list.map { it.name }.contains(
                        "default")
        )
    }
}