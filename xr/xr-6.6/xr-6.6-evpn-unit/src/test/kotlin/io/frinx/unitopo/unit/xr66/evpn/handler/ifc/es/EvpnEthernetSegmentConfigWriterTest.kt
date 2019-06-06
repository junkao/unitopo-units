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

package io.frinx.unitopo.unit.xr7.evpn.handler.ifc.es

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.evpn.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.EthernetSegmentLoadBalance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.Evpn
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.EvpnTables
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.EvpnInterfaces
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.interfaces.EvpnInterface
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.interfaces.EvpnInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.interfaces.EvpnInterfaceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.interfaces.evpn._interface.EthernetSegmentBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.identifier.IdentifierBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.HexInteger
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.EthernetSegment
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.ethernet.segment.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.ethernet.segment.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.types.rev181112.EthernetSegmentIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.types.rev181112.PORTACTIVE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.types.rev181112.SINGLEACTIVE
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.EthernetSegmentIdentifier as NativeESI
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.interfaces.evpn._interface.EthernetSegment as NativeEthernetSegment

class EvpnEthernetSegmentConfigWriterTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: EvpnEthernetSegmentConfigWriter

    companion object {
        val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        val IID_ES_CONFIG = IIDs.EV_INTERFACES
            .child(Interface::class.java, InterfaceKey("Bundle-Ether65535"))
            .child(EthernetSegment::class.java)
            .child(Config::class.java)
        val NATIVE_IID = InstanceIdentifier.create(Evpn::class.java)
            .child(EvpnTables::class.java)
            .child(EvpnInterfaces::class.java)
            .child(EvpnInterface::class.java, EvpnInterfaceKey(InterfaceName("Bundle-Ether65535")))
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = Mockito.spy(EvpnEthernetSegmentConfigWriter(underlayAccess))
    }

    @Test
    fun testWriteCurrentAttributes() {
        val data = ConfigBuilder().apply {
            this.identifier = EthernetSegmentIdentifier("11:22:33:44:55:66:77:88:99")
            this.bgpRouteTarget = MacAddress("00:ff:11:22:00:ff")
            this.loadBalancingMode = PORTACTIVE::class.java
        }.build()
        val expected = EvpnInterfaceBuilder().apply {
            this.interfaceName = InterfaceName("Bundle-Ether65535")
            this.ethernetSegment = EthernetSegmentBuilder().apply {
                this.identifier = IdentifierBuilder().apply {
                    this.bytes01 = HexInteger("11")
                    this.bytes23 = HexInteger("2233")
                    this.bytes45 = HexInteger("4455")
                    this.bytes67 = HexInteger("6677")
                    this.bytes89 = HexInteger("8899")
                    this.type = NativeESI.Type0
                }.build()
                this.esImportRouteTarget = MacAddress("00:ff:11:22:00:ff")
                this.loadBalancingMode = EthernetSegmentLoadBalance.PortActive
                this.setEnable(true)
            }.build()
        }.build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<EvpnInterface>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<EvpnInterface>

        Mockito.doNothing().`when`(underlayAccess).merge(Mockito.any(), Mockito.any())

        target.writeCurrentAttributes(IID_ES_CONFIG, data, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).merge(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<EvpnInterface>>
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(expected)
        )
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val dataBefore = ConfigBuilder().build()
        val dataAfter = ConfigBuilder().apply {
            this.identifier = EthernetSegmentIdentifier("00:ff:00:aa:bb:cc:dd:00:88")
            this.bgpRouteTarget = MacAddress("11:aa:11:12:34:ef")
            this.loadBalancingMode = SINGLEACTIVE::class.java
        }.build()
        val expected = EvpnInterfaceBuilder().apply {
            this.interfaceName = InterfaceName("Bundle-Ether65535")
            this.ethernetSegment = EthernetSegmentBuilder().apply {
                this.identifier = IdentifierBuilder().apply {
                    this.bytes01 = HexInteger("00")
                    this.bytes23 = HexInteger("ff00")
                    this.bytes45 = HexInteger("aabb")
                    this.bytes67 = HexInteger("ccdd")
                    this.bytes89 = HexInteger("0088")
                    this.type = NativeESI.Type0
                }.build()
                this.esImportRouteTarget = MacAddress("11:aa:11:12:34:ef")
                this.loadBalancingMode = EthernetSegmentLoadBalance.SingleActive
                this.setEnable(true)
            }.build()
        }.build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<EvpnInterface>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<EvpnInterface>

        Mockito.doNothing().`when`(underlayAccess).merge(Mockito.any(), Mockito.any())

        target.updateCurrentAttributes(IID_ES_CONFIG, dataBefore, dataAfter, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).merge(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<EvpnInterface>>
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(expected)
        )
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val data = ConfigBuilder().build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<NativeEthernetSegment>>

        Mockito.doNothing().`when`(underlayAccess).delete(Mockito.any())

        // test
        target.deleteCurrentAttributes(IID_ES_CONFIG, data, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).delete(idCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID.child(NativeEthernetSegment::class.java))
                as Matcher<in InstanceIdentifier<NativeEthernetSegment>>
        )
    }
}