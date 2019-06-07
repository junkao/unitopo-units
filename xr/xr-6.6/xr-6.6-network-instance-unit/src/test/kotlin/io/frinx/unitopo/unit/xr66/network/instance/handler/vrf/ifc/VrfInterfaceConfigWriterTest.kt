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

package io.frinx.unitopo.unit.xr66.network.instance.handler.vrf.ifc

import com.google.common.base.Optional
import com.google.common.util.concurrent.CheckedFuture
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurationsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev180615.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev180615.InterfaceConfiguration1Builder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces._interface.ConfigBuilder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier
import io.frinx.unitopo.unit.xr66.network.instance.handler.vrf.L3VrfReaderTest as BaseTest

class VrfInterfaceConfigWriterTest {

    @Mock
    private lateinit var ctx: WriteContext

    @Mock
    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var writer: VrfInterfaceConfigWriter

    private val data = ConfigBuilder().setId(BaseTest.BUN_ETH_301_1).build()

    private val idCap = ArgumentCaptor
        .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<InterfaceConfiguration>>

    private val dataCap = ArgumentCaptor
        .forClass(DataObject::class.java) as ArgumentCaptor<InterfaceConfiguration>

    companion object {
        private val NATIVE_ACT = InterfaceActive("act")
        private val NATIVE_IFC_NAME = InterfaceName(BaseTest.BUN_ETH_301_1)

        private val IID_CONFIG = BaseTest.IID_NETWORK_INSTANCE
            .child(Interfaces::class.java)
            .child(Interface::class.java, InterfaceKey(BaseTest.BUN_ETH_301_1))
            .child(Config::class.java)

        private val NATIVE_IID: KeyedInstanceIdentifier<InterfaceConfiguration, InterfaceConfigurationKey> =
            KeyedInstanceIdentifier
                .create(InterfaceConfigurations::class.java)
                .child(InterfaceConfiguration::class.java,
                    InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IFC_NAME))

        private val READ_DATA = InterfaceConfigurationsBuilder()
            .setInterfaceConfiguration(listOf(InterfaceConfigurationBuilder().apply {
                this.active = NATIVE_ACT
                this.interfaceName = NATIVE_IFC_NAME
                this.addAugmentation(InterfaceConfiguration1::class.java, InterfaceConfiguration1Builder()
                    .setVrf(CiscoIosXrString(BaseTest.VRF_IM1))
                    .build())
            }.build())
        ).build()
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        writer = VrfInterfaceConfigWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        Mockito.doNothing().`when`(underlayAccess).safePut(Mockito.any(), Mockito.any())
        val future = Mockito.mock(CheckedFuture::class.java)
            as CheckedFuture<out Optional<out DataObject>, ReadFailedException>
        Mockito.`when`(future.checkedGet()).thenReturn(Optional.of(READ_DATA))
        Mockito.`when`(underlayAccess
            .read(Mockito.any(InstanceIdentifier::class.java), Mockito.any())).thenReturn(future)

        writer.writeCurrentAttributes(IID_CONFIG, data, ctx)

        Mockito.verify(underlayAccess, Mockito.times(1)).safePut(idCap.capture(), dataCap.capture())

        Assert.assertEquals(1, idCap.allValues.size)
        Assert.assertEquals(1, dataCap.allValues.size)

        Assert.assertEquals(NATIVE_IID, idCap.allValues[0])

        val expectedConfig = InterfaceConfigurationBuilder().apply {
            this.active = NATIVE_ACT
            this.interfaceName = NATIVE_IFC_NAME
            this.addAugmentation(InterfaceConfiguration1::class.java, InterfaceConfiguration1Builder()
                .setVrf(CiscoIosXrString(BaseTest.VRF_IM1))
                .build())
        }.build()

        Assert.assertEquals(expectedConfig, dataCap.allValues[0])
    }

    @Test
    fun testDeleteCurrentAttributes() {
        Mockito.doNothing().`when`(underlayAccess).safeDelete(Mockito.any(), Mockito.any())

        writer.deleteCurrentAttributes(IID_CONFIG, data, ctx)

        Mockito.verify(underlayAccess, Mockito.times(1)).safeDelete(idCap.capture(), dataCap.capture())

        Assert.assertEquals(1, idCap.allValues.size)
        Assert.assertEquals(NATIVE_IID, idCap.allValues[0])
    }
}