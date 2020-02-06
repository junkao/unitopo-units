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

package io.frinx.unitopo.unit.xr7.bgp.handler.peergroup

import com.google.common.base.Optional
import com.google.common.util.concurrent.CheckedFuture
import io.fd.honeycomb.translate.util.RWUtils
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
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.Bgp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.InstanceAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.InstanceAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.FourByteAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.four._byte.`as`.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.BgpEntity
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.NeighborGroups
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.NeighborGroup
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.NeighborGroupKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.neighbor.group.NeighborGroupAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.neighbor.group.neighbor.group.afs.NeighborGroupAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.neighbor.group.neighbor.group.afs.NeighborGroupAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev190405.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.neighbor.groups.neighbor.group.neighbor.group.afs.NeighborGroupAfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev190405.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev190405.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.afi.safi.list.afi.safi.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.afi.safi.list.afi.safi.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.peer.group.base.AfiSafis
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.L3VPNIPV4UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.ConfigBuilder as GlobalConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.BgpBuilder as OcBgpBuilder

class PeerGroupAfiSafiConfigWriterTest : AbstractNetconfHandlerTest() {

    @Mock
    private lateinit var underlayAccess: UnderlayAccess
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var target: PeerGroupAfiSafiConfigWriter

    companion object {
        private val IID_CONFIG = PeerGroupListReaderTest.id
            .child(AfiSafis::class.java)
            .child(AfiSafi::class.java, AfiSafiKey(L3VPNIPV4UNICAST::class.java))
            .child(Config::class.java)
        private val NATIVE_IID = KeyedInstanceIdentifier.create(Bgp::class.java)!!
            .child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))
            .child(InstanceAs::class.java, InstanceAsKey(BgpAsRange(0)))
            .child(FourByteAs::class.java, FourByteAsKey(BgpAsRange(19999)))
            .child(DefaultVrf::class.java)
            .child(BgpEntity::class.java)
            .child(NeighborGroups::class.java)
            .child(NeighborGroup::class.java,
                NeighborGroupKey(CiscoIosXrString(PeerGroupListReaderTest.PEER_GROUP_NAME1)))
            .child(NeighborGroupAfs::class.java)
            .child(NeighborGroupAf::class.java, NeighborGroupAfKey(BgpAddressFamily.Vpnv4Unicast))
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        target = PeerGroupAfiSafiConfigWriter(underlayAccess!!)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val config = ConfigBuilder()
            .setAfiSafiName(L3VPNIPV4UNICAST::class.java)
            .build()
        val protocol = ProtocolBuilder().apply {
            bgp = OcBgpBuilder().apply {
                global = GlobalBuilder().apply {
                    this.config = GlobalConfigBuilder().apply {
                        `as` = AsNumber(19999L)
                    }.build()
                }.build()
            }.build()
        }.build()
        Mockito.`when`(writeContext?.readAfter(RWUtils.cutId(IID_CONFIG, Protocol::class.java)))
            .thenReturn(Optional.of(protocol))

        // read
        val future = Mockito.mock(CheckedFuture::class.java)
            as CheckedFuture<out Optional<out DataObject>, ReadFailedException>
        Mockito.`when`(underlayAccess
            .read(Mockito.any(InstanceIdentifier::class.java))).thenReturn(future)
        Mockito.`when`(future.checkedGet()).thenReturn(Optional.of(NeighborGroupAfBuilder().build()))
        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<Config>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<Config>
        Mockito.doNothing().`when`(underlayAccess!!).safePut(Mockito.any(), Mockito.any())
        target!!.writeCurrentAttributes(IID_CONFIG, config, writeContext!!)
        // capture
        Mockito.verify(underlayAccess!!, Mockito.times(1)).safePut(idCap.capture(), dataCap.capture())
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(
                idCap.allValues.get(0),
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<Config>>
        )
    }

    @Test
    fun testdeleteCurrentAttributes() {
        val config = ConfigBuilder()
            .setAfiSafiName(L3VPNIPV4UNICAST::class.java)
            .build()
        val protocol = ProtocolBuilder().apply {
            bgp = OcBgpBuilder().apply {
                global = GlobalBuilder().apply {
                    this.config = GlobalConfigBuilder().apply {
                        `as` = AsNumber(19999L)
                    }.build()
                }.build()
            }.build()
        }.build()
        Mockito.`when`(writeContext?.readBefore(RWUtils.cutId(IID_CONFIG, Protocol::class.java)))
            .thenReturn(Optional.of(protocol))
        // read
        val future = Mockito.mock(CheckedFuture::class.java)
            as CheckedFuture<out Optional<out DataObject>, ReadFailedException>
        Mockito.`when`(underlayAccess
            .read(Mockito.any(InstanceIdentifier::class.java))).thenReturn(future)
        Mockito.`when`(future.checkedGet()).thenReturn(Optional.of(NeighborGroupAfBuilder().build()))

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<Config>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<Config>

        Mockito.doNothing().`when`(underlayAccess!!).safeDelete(Mockito.any(), Mockito.any())
        target!!.deleteCurrentAttributes(IID_CONFIG, config, writeContext!!)
        // capture
        Mockito.verify(underlayAccess!!,
            Mockito.times(1)).safeDelete(idCap.capture(), dataCap.capture())
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(
                idCap.allValues.get(0),
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<Config>>
        )
    }
}