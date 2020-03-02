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

import com.google.common.base.Optional
import com.google.common.util.concurrent.CheckedFuture
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
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
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.MaxLinkMetrics
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.MaxLinkMetricsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.max.link.metrics.MaxLinkMetricBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev190405.IsisInstanceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisGlobalConfAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisGlobalConfAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisInternalLevel
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.global.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.global.base.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.top.Isis
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.top.isis.Global
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.ISIS
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev190405.IsisInternalLevel as UnderlayIILevel

class IsisGlobalConfigWriterTest : AbstractNetconfHandlerTest() {

    @Mock
    private lateinit var underlayAccess: UnderlayAccess
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var target: IsisGlobalConfigWriter

    companion object {
        private val IID_CONFIG = KeyedInstanceIdentifier
                .create(NetworkInstances::class.java)
                .child(NetworkInstance::class.java, NetworkInstanceKey("default"))
                .child(Protocols::class.java)
                .child(Protocol::class.java, ProtocolKey(ISIS::class.java, "default"))
                .child(Isis::class.java)
                .child(Global::class.java)
                .child(Config::class.java)
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        target = IsisGlobalConfigWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val config = ConfigBuilder()
            .addAugmentation(
                IsisGlobalConfAug::class.java,
                IsisGlobalConfAugBuilder().setMaxLinkMetric(
                    listOf(IsisInternalLevel.LEVEL1, IsisInternalLevel.LEVEL2)
                ).build()
            ).build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<MaxLinkMetrics>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<MaxLinkMetrics>
        Mockito.doNothing().`when`(underlayAccess).safePut(Mockito.any(), Mockito.any())
        target.writeCurrentAttributes(IID_CONFIG, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).safePut(idCap.capture(), dataCap.capture())

        // assert times
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // assert parameter
        val expectedIid = IsisProtocolReader.UNDERLAY_ISIS
            .child(Instance::class.java, InstanceKey(IsisInstanceName("default")))
            .child(MaxLinkMetrics::class.java)
        val expectedData = MaxLinkMetricsBuilder().setMaxLinkMetric(
            listOf(
                MaxLinkMetricBuilder().setLevel(UnderlayIILevel.Level1).build(),
                MaxLinkMetricBuilder().setLevel(UnderlayIILevel.Level2).build()
            )
        ).build()
        Assert.assertThat(
            idCap.allValues.get(0),
            CoreMatchers.equalTo(expectedIid) as Matcher<in InstanceIdentifier<MaxLinkMetrics>>
        )
        Assert.assertThat(
            dataCap.allValues.get(0),
            CoreMatchers.equalTo(expectedData) as Matcher<in MaxLinkMetrics>
        )
    }

    @Test
    fun testUpdateCurrentAttributesForType() {
        val configBefore = ConfigBuilder()
                .build()
        var configAfter = ConfigBuilder()
            .addAugmentation(
                IsisGlobalConfAug::class.java,
                IsisGlobalConfAugBuilder().setMaxLinkMetric(
                    listOf(IsisInternalLevel.LEVEL1, IsisInternalLevel.LEVEL2)
                ).build()
            ).build()

        val future = Mockito.mock(CheckedFuture::class.java)
            as CheckedFuture<out Optional<out DataObject>, ReadFailedException>
        Mockito.`when`(underlayAccess
            .read(Mockito.any(InstanceIdentifier::class.java))).thenReturn(future)
        Mockito.`when`(future.checkedGet()).thenReturn(Optional.of(MaxLinkMetricsBuilder().build()))
        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<MaxLinkMetrics>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<MaxLinkMetrics>
        Mockito.doNothing().`when`(underlayAccess).safeMerge(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())
        target.updateCurrentAttributes(IID_CONFIG, configBefore, configAfter, writeContext)

        Mockito.verify(underlayAccess, Mockito.times(1))
            .safeMerge(Mockito.any(), Mockito.any(), idCap.capture(), dataCap.capture())

        // assert times
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // assert parameter
        val expectedIid = IsisProtocolReader.UNDERLAY_ISIS
            .child(Instance::class.java, InstanceKey(IsisInstanceName("default")))
            .child(MaxLinkMetrics::class.java)
        val expectedData = MaxLinkMetricsBuilder().setMaxLinkMetric(
            listOf(
                MaxLinkMetricBuilder().setLevel(UnderlayIILevel.Level1).build(),
                MaxLinkMetricBuilder().setLevel(UnderlayIILevel.Level2).build()
            )
        ).build()
        Assert.assertThat(
            idCap.allValues.get(0),
            CoreMatchers.equalTo(expectedIid) as Matcher<in InstanceIdentifier<MaxLinkMetrics>>
        )
        Assert.assertThat(
            dataCap.allValues.get(0),
            CoreMatchers.equalTo(expectedData) as Matcher<in MaxLinkMetrics>
        )
    }

    @Test
    fun testdeleteCurrentAttributes() {
        val future = Mockito.mock(CheckedFuture::class.java)
            as CheckedFuture<out Optional<out DataObject>, ReadFailedException>
        Mockito.`when`(underlayAccess
            .read(Mockito.any(InstanceIdentifier::class.java))).thenReturn(future)
        Mockito.`when`(future.checkedGet()).thenReturn(Optional.of(MaxLinkMetricsBuilder().build()))

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<MaxLinkMetrics>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<MaxLinkMetrics>

        Mockito.doNothing().`when`(underlayAccess).safeDelete(Mockito.any(), Mockito.any())
        target.deleteCurrentAttributes(IID_CONFIG, ConfigBuilder().build(), writeContext)
        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).safeDelete(idCap.capture(), dataCap.capture())
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))

        val expectedIid = IsisProtocolReader.UNDERLAY_ISIS
            .child(Instance::class.java, InstanceKey(IsisInstanceName("default")))
            .child(MaxLinkMetrics::class.java)
        Assert.assertThat(
            idCap.allValues.get(0),
            CoreMatchers.equalTo(expectedIid) as Matcher<in InstanceIdentifier<MaxLinkMetrics>>
        )
    }
}