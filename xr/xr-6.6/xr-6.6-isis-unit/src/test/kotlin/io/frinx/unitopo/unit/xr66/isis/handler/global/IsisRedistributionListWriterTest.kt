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

package io.frinx.unitopo.unit.xr66.isis.handler.global

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import io.frinx.unitopo.unit.xr66.isis.handler.IsisProtocolReader
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.IsisConfigurableLevels
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.IsisRedistProto
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.instance.afs.af.af.content.redistributions.redistribution.OspfOrOspfv3OrIsisOrApplication
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.instance.afs.af.af.content.redistributions.redistribution.OspfOrOspfv3OrIsisOrApplicationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev170501.IsisAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev170501.IsisInstanceName
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev170501.IsisSubAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.Af1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.Redistributions
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.Redistribution
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.RedistributionBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.RedistributionKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.redistribution.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.IPV6
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.LevelType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.Af
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.AfKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.global.base.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.top.Isis
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.top.isis.Global
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.ISIS
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.instance.Afs as UlAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.instance.afs.Af as UlAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.instance.afs.AfKey as UlAfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.instance.afs.af.AfData as UlAfData
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.instance.afs.af.af.content.Redistributions as UlRedistributions
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.instance.afs.af.af.content.redistributions.Redistribution as UlRedistribution
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.instance.afs.af.af.content.redistributions.RedistributionKey as UlRedistributionKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class IsisRedistributionListWriterTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var underlayAccess: UnderlayAccess

    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var target: IsisRedistributionListWriter

    companion object {
        val IID_REDI = IID
            .create(NetworkInstances::class.java)
            .child(NetworkInstance::class.java, NetworkInstanceKey("default"))
            .child(Protocols::class.java)
            .child(Protocol::class.java, ProtocolKey(ISIS::class.java, "ISIS-001"))
            .child(Isis::class.java)
            .child(Global::class.java)
            .child(AfiSafi::class.java)
            .child(Af::class.java, AfKey(IPV6::class.java, UNICAST::class.java))
            .augmentation(Af1::class.java)
            .child(Redistributions::class.java)
            .child(Redistribution::class.java, RedistributionKey("400", "isis"))
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper("/data_nodes.xml"))
        target = IsisRedistributionListWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val af = RedistributionBuilder()
            .setInstance("400")
            .setProtocol("isis")
            .setConfig(
                ConfigBuilder()
                    .setInstance("400")
                    .setProtocol("isis")
                    .setLevel(LevelType.LEVEL1)
                    .setRoutePolicy("rp-1")
                    .build()
            ).build()
        val idCap = ArgumentCaptor
            .forClass(IID::class.java) as ArgumentCaptor<IID<OspfOrOspfv3OrIsisOrApplication>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<OspfOrOspfv3OrIsisOrApplication>
        Mockito.doNothing().`when`(underlayAccess).safePut(Mockito.any(), Mockito.any())
        target.writeCurrentAttributes(IID_REDI, af, writeContext)
        Mockito.verify(underlayAccess, Mockito.times(1))
            .safePut(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))
        // verify parameter to underlay
        val expectedId = IsisProtocolReader.UNDERLAY_ISIS
            .child(Instance::class.java, InstanceKey(IsisInstanceName("ISIS-001")))
            .child(UlAfs::class.java)
            .child(UlAf::class.java, UlAfKey(IsisAddressFamily.Ipv6, IsisSubAddressFamily.Unicast))
            .child(UlAfData::class.java)
            .child(UlRedistributions::class.java)
            .child(UlRedistribution::class.java, UlRedistributionKey(IsisRedistProto.Isis))
            .child(OspfOrOspfv3OrIsisOrApplication::class.java,
                OspfOrOspfv3OrIsisOrApplicationKey(CiscoIosXrString("400")))
        Assert.assertThat(idCap.allValues.get(0),
            CoreMatchers.equalTo(expectedId) as Matcher<in IID<OspfOrOspfv3OrIsisOrApplication>>)
        val actual = dataCap.allValues.get(0)
        Assert.assertEquals("400", actual.instanceName.value)
        Assert.assertEquals(IsisConfigurableLevels.Level1, actual.levels)
        Assert.assertEquals("rp-1", actual.routePolicyName.value)
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val afBefore = RedistributionBuilder().build()
        val af = RedistributionBuilder()
            .setInstance("400")
            .setProtocol("isis")
            .setConfig(
                ConfigBuilder()
                    .setInstance("400")
                    .setProtocol("isis")
                    .setLevel(LevelType.LEVEL1)
                    .setRoutePolicy("rp-1")
                    .build()
            ).build()
        val idCap = ArgumentCaptor
            .forClass(IID::class.java) as ArgumentCaptor<IID<OspfOrOspfv3OrIsisOrApplication>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<OspfOrOspfv3OrIsisOrApplication>

        Mockito.doNothing().`when`(underlayAccess).safeMerge(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())
        target.updateCurrentAttributes(IID_REDI, afBefore, af, writeContext)
        Mockito.verify(underlayAccess, Mockito.times(1))
            .safeMerge(Mockito.any(), Mockito.any(), idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))
        // verify parameter to underlay
        val expectedId = IsisProtocolReader.UNDERLAY_ISIS
            .child(Instance::class.java, InstanceKey(IsisInstanceName("ISIS-001")))
            .child(UlAfs::class.java)
            .child(UlAf::class.java, UlAfKey(IsisAddressFamily.Ipv6, IsisSubAddressFamily.Unicast))
            .child(UlAfData::class.java)
            .child(UlRedistributions::class.java)
            .child(UlRedistribution::class.java, UlRedistributionKey(IsisRedistProto.Isis))
            .child(OspfOrOspfv3OrIsisOrApplication::class.java,
                OspfOrOspfv3OrIsisOrApplicationKey(CiscoIosXrString("400")))
        Assert.assertThat(idCap.allValues.get(0),
            CoreMatchers.equalTo(expectedId) as Matcher<in IID<OspfOrOspfv3OrIsisOrApplication>>)
        val actual = dataCap.allValues.get(0)
        Assert.assertEquals("400", actual.instanceName.value)
        Assert.assertEquals(IsisConfigurableLevels.Level1, actual.levels)
        Assert.assertEquals("rp-1", actual.routePolicyName.value)
    }

    @Test
    fun testdeleteCurrentAttributes() {
        val af = RedistributionBuilder()
            .setInstance("400")
            .setProtocol("isis")
            .setConfig(
                ConfigBuilder()
                    .setInstance("400")
                    .setProtocol("isis")
                    .setLevel(LevelType.LEVEL1)
                    .setRoutePolicy("rp-1")
                    .build()
            ).build()

        val idCap = ArgumentCaptor
            .forClass(IID::class.java) as ArgumentCaptor<IID<OspfOrOspfv3OrIsisOrApplication>>
        Mockito.doNothing().`when`(underlayAccess).safeDelete(Mockito.any(), Mockito.any())
        target.deleteCurrentAttributes(IID_REDI, af, writeContext)
        Mockito.verify(underlayAccess, Mockito.times(1))
            .safeDelete(idCap.capture(), Mockito.any())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        // verify parameter to underlay
        val expectedId = IsisProtocolReader.UNDERLAY_ISIS
            .child(Instance::class.java, InstanceKey(IsisInstanceName("ISIS-001")))
            .child(UlAfs::class.java)
            .child(UlAf::class.java, UlAfKey(IsisAddressFamily.Ipv6, IsisSubAddressFamily.Unicast))
            .child(UlAfData::class.java)
            .child(UlRedistributions::class.java)
            .child(UlRedistribution::class.java, UlRedistributionKey(IsisRedistProto.Isis))
            .child(OspfOrOspfv3OrIsisOrApplication::class.java,
                OspfOrOspfv3OrIsisOrApplicationKey(CiscoIosXrString("400")))
        Assert.assertThat(idCap.allValues.get(0),
            CoreMatchers.equalTo(expectedId) as Matcher<in IID<OspfOrOspfv3OrIsisOrApplication>>)
    }
}