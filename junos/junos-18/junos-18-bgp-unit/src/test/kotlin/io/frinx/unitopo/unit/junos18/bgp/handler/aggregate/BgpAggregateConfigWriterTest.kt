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

package io.frinx.unitopo.unit.junos18.bgp.handler.aggregate

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.bgp.handler.BgpProtocolReader
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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.NiProtAggAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.extension.rev180323.NiProtAggAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.LocalAggregates
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.Aggregate
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.AggregateKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpPrefixBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.common.types.rev180101.PolicyAlgebra
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.juniper.routing.instance.RoutingOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.rib_aggregate_type.Route
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.rib_aggregate_type.RouteBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.rib_aggregate_type.RouteKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.Instance
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.InstanceKey
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.common.types.rev180101.Ipprefix as JunosIpprefix
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.juniper.routing.options.Aggregate as JunosAggregate

class BgpAggregateConfigWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: BgpAggregateConfigWriter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = Mockito.spy(BgpAggregateConfigWriter(underlayAccess))
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun writeCurrentAttributesForType() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG)
                .build()
        val expectedConfig = RouteBuilder(NATIVE_CONFIG)
                .build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<Route>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<Route>

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        target.writeCurrentAttributesWResult(id, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<Route>>
        )
        Assert.assertThat(
                dataCap.allValues[0],
                CoreMatchers.equalTo(expectedConfig)
        )
    }

    @Test
    fun updateCurrentAttributesForType() {
        val id = IID_CONFIG
        val configBefore = ConfigBuilder(CONFIG)
                .build()
        val configAfter = ConfigBuilder(CONFIG)
                .build()

        Mockito.doReturn(true).`when`(target).writeCurrentAttributesWResult(id, configAfter, writeContext)

        target.updateCurrentAttributesWResult(id, configBefore, configAfter, writeContext)

        Mockito.verify(target).writeCurrentAttributesWResult(id, configAfter, writeContext)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun deleteCurrentAttributesForType() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG)
                .build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<Route>>

        Mockito.doNothing().`when`(underlayAccess).delete(Mockito.any())

        target.deleteCurrentAttributesWResult(id, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).delete(idCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<Route>>
        )
    }

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")

        private val VRF_NAME = "BLIZZARD"
        private val PREFIX = "10.10.10.10/32"
        private val POLICY1 = "AGGREGATE1-1"
        private val POLICY2 = "AGGREGATE1-2"
        private val APPLY_POLICY = listOf(POLICY1, POLICY2)

        private val IID_CONFIG = IIDs.NETWORKINSTANCES
                .child(NetworkInstance::class.java, NetworkInstanceKey(VRF_NAME))
                .child(Protocols::class.java)
                .child(Protocol::class.java, ProtocolKey(BGP::class.java, "default"))
                .child(LocalAggregates::class.java)
                .child(Aggregate::class.java, AggregateKey(IpPrefixBuilder.getDefaultInstance(PREFIX)))
                .child(Config::class.java)

        private val AUG = NiProtAggAugBuilder()
                .setSummaryOnly(true)
                .setApplyPolicy(listOf(POLICY1, POLICY2))
                .build()

        private val CONFIG = ConfigBuilder()
                .setPrefix(IpPrefixBuilder.getDefaultInstance(PREFIX))
                .addAugmentation(NiProtAggAug::class.java, AUG)
                .build()

        private val NATIVE_IID = BgpProtocolReader.JUNOS_VRFS_ID
                .child(Instance::class.java, InstanceKey(VRF_NAME))
                .child(RoutingOptions::class.java)
                .child(JunosAggregate::class.java)
                .child(Route::class.java, RouteKey(JunosIpprefix.getDefaultInstance(PREFIX)))

        private val NATIVE_CONFIG = RouteBuilder()
                .setPolicy(APPLY_POLICY.map { PolicyAlgebra(it) })
                .setName(JunosIpprefix.getDefaultInstance(PREFIX))
                .build()
    }
}