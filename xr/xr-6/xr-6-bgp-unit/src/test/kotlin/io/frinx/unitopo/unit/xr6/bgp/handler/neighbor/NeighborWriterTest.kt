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

package io.frinx.unitopo.unit.xr6.bgp.handler.neighbor

import com.google.common.base.Optional
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.UnderlayNeighbor
import io.frinx.unitopo.unit.xr6.bgp.UnderlayNeighborKey
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.Bgp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.BgpEntity
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.AfiSafisBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.ConfigBuilder as NeighborConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.BgpBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.Neighbors
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.IPV4UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.apply.policy.group.ApplyPolicyBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.routing.policy.rev170714.apply.policy.group.apply.policy.ConfigBuilder as ApplyPolicyConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv4Address
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.bgp.entity.Neighbors as UnderlayNeighbors

class NeighborWriterTest {

    @Mock
    private lateinit var underlayAccess: UnderlayAccess
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var writer: NeighborWriter

    companion object {

        private val CONFIG = NeighborBuilder().setConfig(NeighborConfigBuilder().setPeerAs(AsNumber(1024L)).build())
            .setKey(NeighborKey(IpAddress(Ipv4Address("99.0.0.99"))))
            .build()

        private val BGP = Optional.of(BgpBuilder()
            .setGlobal(GlobalBuilder()
                .setAfiSafis(AfiSafisBuilder().setAfiSafi(listOf(AfiSafiBuilder()
                    .setAfiSafiName(IPV4UNICAST::class.java).build())).build())
            .setConfig(ConfigBuilder().setAs(AsNumber(666L)).build()).build()).build())

        private val NATIVE_IID = InstanceIdentifier
            .create(Bgp::class.java)!!
            .child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))
            .child(InstanceAs::class.java, InstanceAsKey(BgpAsRange(0L)))
            .child(FourByteAs::class.java, FourByteAsKey(BgpAsRange(666L)))
            .child(DefaultVrf::class.java)
            .child(BgpEntity::class.java)
            .child(UnderlayNeighbors::class.java)
            .child(UnderlayNeighbor::class.java, UnderlayNeighborKey(IpAddressNoZone(Ipv4AddressNoZone("99.0.0.99"))))

        private val IID_NEIGHBOR = KeyedInstanceIdentifier
            .create(NetworkInstances::class.java)
            .child(NetworkInstance::class.java, NetworInstance.DEFAULT_NETWORK)
            .child(Protocols::class.java)
            .child(Protocol::class.java,
                ProtocolKey(org.opendaylight.yang.gen.v1.http
                    .frinx.openconfig.net.yang.policy.types.rev160512.BGP::class.java, "default"))
            .child(org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202
                .bgp.top.Bgp::class.java)
            .child(Neighbors::class.java)
            .child(Neighbor::class.java, NeighborKey(IpAddress(Ipv4Address("99.0.0.99"))))
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        writer = NeighborWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val idCap = ArgumentCaptor.forClass(InstanceIdentifier::class.java)
            as ArgumentCaptor<KeyedInstanceIdentifier<UnderlayNeighbor, UnderlayNeighborKey>>
        val dataCap = ArgumentCaptor.forClass(DataObject::class.java)
            as ArgumentCaptor<UnderlayNeighbor>
        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())
        Mockito.doReturn(BGP).`when`(writeContext).readAfter(Mockito.any(InstanceIdentifier::class.java))
        writer.writeCurrentAttributes(IID_NEIGHBOR, CONFIG, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).merge(idCap.capture(), dataCap.capture())
        Assert.assertEquals(idCap.allValues.size, 1)
        Assert.assertEquals(dataCap.allValues.size, 1)
        Assert.assertEquals(idCap.allValues[0], NATIVE_IID)
        val neighborAfter = dataCap.allValues[0]
        Assert.assertEquals("99.0.0.99", neighborAfter.neighborAddress.ipv4AddressNoZone.value)
        Assert.assertEquals(0, neighborAfter.remoteAs.asXx.value)
        Assert.assertEquals(1024L, neighborAfter.remoteAs.asYy.value)
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val idCap = ArgumentCaptor.forClass(KeyedInstanceIdentifier::class.java)
            as ArgumentCaptor<KeyedInstanceIdentifier<UnderlayNeighbor, UnderlayNeighborKey>>
        Mockito.doReturn(BGP).`when`(writeContext).readBefore(Mockito.any(InstanceIdentifier::class.java))
        Mockito.doNothing().`when`(underlayAccess).delete(Mockito.any())
        writer.deleteCurrentAttributes(IID_NEIGHBOR, CONFIG, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).delete(idCap.capture())
        Assert.assertEquals(idCap.allValues.size, 1)
        Assert.assertEquals(idCap.allValues[0], NATIVE_IID)
    }

    @Test
    fun testUpdateCurrentAttributesForType() {
        val configAfter = NeighborBuilder().setConfig(NeighborConfigBuilder().setPeerAs(AsNumber(1024L)).build())
            .setKey(NeighborKey(IpAddress(Ipv4Address("99.0.0.99"))))
            .setApplyPolicy(ApplyPolicyBuilder().setConfig(ApplyPolicyConfigBuilder()
                .setExportPolicy(listOf("nexthopself"))
                .setImportPolicy(listOf("policy2"))
                .build()).build())
            .build()

        val idCap = ArgumentCaptor
            .forClass(KeyedInstanceIdentifier::class.java)
            as ArgumentCaptor<KeyedInstanceIdentifier<UnderlayNeighbor, UnderlayNeighborKey>>

        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<UnderlayNeighbor>
        Mockito.doReturn(BGP).`when`(writeContext).readAfter(Mockito.any(InstanceIdentifier::class.java))
        Mockito.doNothing().`when`(underlayAccess).safeMerge(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())

        writer.updateCurrentAttributes(IID_NEIGHBOR, CONFIG, configAfter, writeContext)
        Mockito.verify(underlayAccess, Mockito.times(1)).safeMerge(idCap.capture(),
            dataCap.capture(), idCap.capture(), dataCap.capture())
        Assert.assertEquals(idCap.allValues[0], NATIVE_IID)
        val neighborAfter = dataCap.allValues[1].neighborAfs.neighborAf?.get(0)
        Assert.assertNotNull(neighborAfter)
        Assert.assertEquals("policy2", neighborAfter?.routePolicyIn)
        Assert.assertEquals(true, neighborAfter?.isNextHopSelf)
    }
}