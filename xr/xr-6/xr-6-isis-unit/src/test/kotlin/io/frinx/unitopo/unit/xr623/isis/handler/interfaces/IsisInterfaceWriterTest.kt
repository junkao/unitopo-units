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

package io.frinx.unitopo.unit.xr623.isis.handler.interfaces

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.Isis
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.IsisConfigurableLevels
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.Instances
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.IsisAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.IsisInstanceName
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.IsisInternalLevel
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.IsisSubAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisIfAfConfAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisIfAfConfAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisIfConfAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisIfConfAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.CircuitType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.IPV6
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.LevelType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._if.global.afi.safi.list.AfBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._interface.group.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._interface.group.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.ISIS
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.utils.IidUtils
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.Interfaces as XrInterfaces
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces.Interface as XrInterface
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces.InterfaceKey as XrInterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._if.global.afi.safi.list.af.ConfigBuilder as AfConfigBuilder

class IsisInterfaceWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext
    private lateinit var underlayAccess: UnderlayAccess
    private lateinit var target: IsisInterfaceWriter

    private lateinit var idCap: List<ArgumentCaptor<IID<DataObject>>>
    private lateinit var dataCap: List<ArgumentCaptor<XrInterface>>

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")

        private val INSTANCE_NAME = "ISIS-001"
        private val INTERFACE_ID = "Bundle-Ether4001"
        private val METRIC = 12345L
        private val METRIC_UPDATED = 54321L

        private val IID_INTERFACE = IidUtils.createIid(IIDs.NE_NE_PR_PR_IS_IN_INTERFACE,
                NetworInstance.DEFAULT_NETWORK,
                ProtocolKey(ISIS::class.java, INSTANCE_NAME),
                InterfaceKey(InterfaceId(INTERFACE_ID)))

        private val CONFIG = ConfigBuilder()
                .setInterfaceId(InterfaceId(INTERFACE_ID))
                .setCircuitType(CircuitType.POINTTOPOINT)
                .addAugmentation(IsisIfConfAug::class.java, IsisIfConfAugBuilder()
                        .setLevelCapability(LevelType.LEVEL1)
                        .build())
                .build()

        private val AF_CONFIG = AfConfigBuilder()
                .setAfiName(IPV6::class.java)
                .setSafiName(UNICAST::class.java)
                .addAugmentation(IsisIfAfConfAug::class.java, IsisIfAfConfAugBuilder()
                        .setMetric(METRIC)
                        .build())
                .build()

        private val INTERFACE = InterfaceBuilder()
                .setInterfaceId(InterfaceId(INTERFACE_ID))
                .setConfig(CONFIG)
                .setAfiSafi(AfiSafiBuilder()
                        .setAf(listOf(AfBuilder()
                                .setAfiName(IPV6::class.java)
                                .setSafiName(UNICAST::class.java)
                                .setConfig(AF_CONFIG)
                                .build()))
                        .build())
                .build()

        private val CONFIG_UPDATED = ConfigBuilder()
                .setInterfaceId(InterfaceId(INTERFACE_ID))
                .addAugmentation(IsisIfConfAug::class.java, IsisIfConfAugBuilder()
                        .setLevelCapability(LevelType.LEVEL2)
                        .build())
                .build()

        private val AF_CONFIG_UPDATED = AfConfigBuilder()
                .setAfiName(IPV6::class.java)
                .setSafiName(UNICAST::class.java)
                .addAugmentation(IsisIfAfConfAug::class.java, IsisIfAfConfAugBuilder()
                        .setMetric(METRIC_UPDATED)
                        .build())
                .build()

        private val INTERFACE_UPDATED = InterfaceBuilder()
                .setInterfaceId(InterfaceId(INTERFACE_ID))
                .setConfig(CONFIG_UPDATED)
                .setAfiSafi(AfiSafiBuilder()
                        .setAf(listOf(AfBuilder()
                                .setAfiName(IPV6::class.java)
                                .setSafiName(UNICAST::class.java)
                                .setConfig(AF_CONFIG_UPDATED)
                                .build()))
                        .build())
                .build()

        private val NATIVE_IID = IID
                .create(Isis::class.java)
                .child(Instances::class.java)
                .child(Instance::class.java, InstanceKey(IsisInstanceName(INSTANCE_NAME)))
                .child(XrInterfaces::class.java)
                .child(XrInterface::class.java, XrInterfaceKey(InterfaceName(INTERFACE_ID)))
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = IsisInterfaceWriter(underlayAccess)

        idCap = listOf(
                ArgumentCaptor.forClass(IID::class.java) as ArgumentCaptor<IID<DataObject>>,
                ArgumentCaptor.forClass(IID::class.java) as ArgumentCaptor<IID<DataObject>>)
        dataCap = listOf(
                ArgumentCaptor.forClass(XrInterface::class.java),
                ArgumentCaptor.forClass(XrInterface::class.java))
    }

    @Test
    fun writeCurrentAttributes() {
        val data = InterfaceBuilder(INTERFACE)
                .build()

        Mockito.doNothing().`when`(underlayAccess).safePut(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(IID_INTERFACE, data, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).safePut(idCap[0].capture(), dataCap[0].capture())

        // verify capture-length
        Assert.assertThat(idCap[0].allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap[0].allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
                idCap[0].allValues[0],
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<DataObject>>
        )
        Assert.assertThat(dataCap[0].allValues[0].interfaceName.value, CoreMatchers.equalTo(INTERFACE_ID))
        Assert.assertThat(dataCap[0].allValues[0].circuitType, CoreMatchers.equalTo(IsisConfigurableLevels.Level1))
        Assert.assertThat(dataCap[0].allValues[0].isPointToPoint, CoreMatchers.`is`(true))
        Assert.assertThat(dataCap[0].allValues[0].isRunning, CoreMatchers.`is`(true))

        Assert.assertThat(dataCap[0].allValues[0].interfaceAfs.interfaceAf!!.size, CoreMatchers.equalTo(1))
        Assert.assertThat(dataCap[0].allValues[0].interfaceAfs.interfaceAf!![0].afName,
                CoreMatchers.equalTo(IsisAddressFamily.Ipv6))
        Assert.assertThat(dataCap[0].allValues[0].interfaceAfs.interfaceAf!![0].safName,
                CoreMatchers.equalTo(IsisSubAddressFamily.Unicast))

        Assert.assertThat(dataCap[0].allValues[0].interfaceAfs.interfaceAf!![0].interfaceAfData.metrics.metric!!.size,
                CoreMatchers.equalTo(1))
        Assert.assertThat(
                dataCap[0].allValues[0].interfaceAfs.interfaceAf!![0].interfaceAfData.metrics.metric!![0].metric,
                CoreMatchers.equalTo(METRIC))
        Assert.assertThat(
                dataCap[0].allValues[0].interfaceAfs.interfaceAf!![0].interfaceAfData.metrics.metric!![0].level,
                CoreMatchers.equalTo(IsisInternalLevel.NotSet))
    }

    @Test
    fun updateCurrentAttributes() {
        val dataBefore = InterfaceBuilder(INTERFACE)
                .build()
        val dataAfter = InterfaceBuilder(INTERFACE_UPDATED)
                .build()

        Mockito.doNothing().`when`(underlayAccess).safeMerge(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())

        // test
        target.updateCurrentAttributes(IID_INTERFACE, dataBefore, dataAfter, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1))
                .safeMerge(idCap[0].capture(), dataCap[0].capture(), idCap[1].capture(), dataCap[1].capture())

        // verify capture-length
        Assert.assertThat(idCap[0].allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap[0].allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(idCap[1].allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap[1].allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
                idCap[0].allValues[0],
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<DataObject>>
        )
        Assert.assertThat(dataCap[0].allValues[0].interfaceName.value, CoreMatchers.equalTo(INTERFACE_ID))
        Assert.assertThat(dataCap[0].allValues[0].circuitType, CoreMatchers.equalTo(IsisConfigurableLevels.Level1))
        Assert.assertThat(dataCap[0].allValues[0].isPointToPoint, CoreMatchers.`is`(true))
        Assert.assertThat(dataCap[0].allValues[0].isRunning, CoreMatchers.`is`(true))

        Assert.assertThat(dataCap[0].allValues[0].interfaceAfs.interfaceAf!!.size, CoreMatchers.equalTo(1))
        Assert.assertThat(dataCap[0].allValues[0].interfaceAfs.interfaceAf!![0].afName,
                CoreMatchers.equalTo(IsisAddressFamily.Ipv6))
        Assert.assertThat(dataCap[0].allValues[0].interfaceAfs.interfaceAf!![0].safName,
                CoreMatchers.equalTo(IsisSubAddressFamily.Unicast))

        Assert.assertThat(dataCap[0].allValues[0].interfaceAfs.interfaceAf!![0].interfaceAfData.metrics.metric!!.size,
                CoreMatchers.equalTo(1))
        Assert.assertThat(
                dataCap[0].allValues[0].interfaceAfs.interfaceAf!![0].interfaceAfData.metrics.metric!![0].metric,
                CoreMatchers.equalTo(METRIC))
        Assert.assertThat(
                dataCap[0].allValues[0].interfaceAfs.interfaceAf!![0].interfaceAfData.metrics.metric!![0].level,
                CoreMatchers.equalTo(IsisInternalLevel.NotSet))

        Assert.assertThat(
                idCap[1].allValues[0],
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<DataObject>>
        )
        Assert.assertThat(dataCap[1].allValues[0].interfaceName.value, CoreMatchers.equalTo(INTERFACE_ID))
        Assert.assertThat(dataCap[1].allValues[0].circuitType, CoreMatchers.equalTo(IsisConfigurableLevels.Level2))
        Assert.assertThat(dataCap[1].allValues[0].isPointToPoint, CoreMatchers.nullValue())
        Assert.assertThat(dataCap[1].allValues[0].isRunning, CoreMatchers.`is`(true))

        Assert.assertThat(dataCap[1].allValues[0].interfaceAfs.interfaceAf!!.size, CoreMatchers.equalTo(1))
        Assert.assertThat(dataCap[1].allValues[0].interfaceAfs.interfaceAf!![0].afName,
                CoreMatchers.equalTo(IsisAddressFamily.Ipv6))
        Assert.assertThat(dataCap[1].allValues[0].interfaceAfs.interfaceAf!![0].safName,
                CoreMatchers.equalTo(IsisSubAddressFamily.Unicast))

        Assert.assertThat(dataCap[1].allValues[0].interfaceAfs.interfaceAf!![0].interfaceAfData.metrics.metric!!.size,
                CoreMatchers.equalTo(1))
        Assert.assertThat(
                dataCap[1].allValues[0].interfaceAfs.interfaceAf!![0].interfaceAfData.metrics.metric!![0].metric,
                CoreMatchers.equalTo(METRIC_UPDATED))
        Assert.assertThat(
                dataCap[1].allValues[0].interfaceAfs.interfaceAf!![0].interfaceAfData.metrics.metric!![0].level,
                CoreMatchers.equalTo(IsisInternalLevel.NotSet))
    }

    @Test
    fun deleteCurrentAttributes() {
        val dataBefore = InterfaceBuilder(INTERFACE)
                .build()
        Mockito.doNothing().`when`(underlayAccess).delete(NATIVE_IID)

        // test
        target.deleteCurrentAttributes(IID_INTERFACE, dataBefore, writeContext)

        // verify
        Mockito.verify(underlayAccess, Mockito.times(1)).delete(NATIVE_IID)
    }
}