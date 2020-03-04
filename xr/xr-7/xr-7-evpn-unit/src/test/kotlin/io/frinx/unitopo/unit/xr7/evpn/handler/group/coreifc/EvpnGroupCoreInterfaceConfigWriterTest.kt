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

package io.frinx.unitopo.unit.xr7.evpn.handler.group.coreifc

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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.Evpn
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.EvpnGroupIdRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.EvpnTables
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.EvpnGroups
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.EvpnGroup
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.EvpnGroupKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.evpn.group.EvpnGroupCoreInterfaces
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.evpn.group.evpn.group.core.interfaces.EvpnGroupCoreInterface
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.evpn.group.evpn.group.core.interfaces.EvpnGroupCoreInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev190405.evpn.evpn.tables.evpn.groups.evpn.group.evpn.group.core.interfaces.EvpnGroupCoreInterfaceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn._interface.part.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn._interface.part.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.Group
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.GroupKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.CoreInterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.core.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.groups.groups.group.core.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class EvpnGroupCoreInterfaceConfigWriterTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: EvpnGroupCoreInterfaceConfigWriter

    companion object {
        val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        val IID_CORE_IFC_CONFIG = IIDs.EV_GROUPS
            .child(Group::class.java, GroupKey(1))
            .child(CoreInterfaces::class.java)
            .child(Interface::class.java, InterfaceKey("Bundle-Ether11001"))
            .child(Config::class.java)
        val NATIVE_IID = InstanceIdentifier.create(Evpn::class.java)
            .child(EvpnTables::class.java)
            .child(EvpnGroups::class.java)
            .child(EvpnGroup::class.java, EvpnGroupKey(EvpnGroupIdRange(1)))
            .child(EvpnGroupCoreInterfaces::class.java)
            .child(EvpnGroupCoreInterface::class.java, EvpnGroupCoreInterfaceKey(InterfaceName("Bundle-Ether11001")))
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = Mockito.spy(EvpnGroupCoreInterfaceConfigWriter(underlayAccess))
    }

    @Test
    fun testWriteCurrentAttributes() {
        val data = ConfigBuilder().apply {
            this.name = "Bundle-Ether11001"
        }.build()
        val expected = EvpnGroupCoreInterfaceBuilder().apply {
            this.interfaceName = InterfaceName("Bundle-Ether11001")
        }.build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<EvpnGroupCoreInterface>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<EvpnGroupCoreInterface>

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        target.writeCurrentAttributes(IID_CORE_IFC_CONFIG, data, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<EvpnGroupCoreInterface>>
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(expected)
        )
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val data = ConfigBuilder().apply {
            this.name = "Bundle-Ether11001"
        }.build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<EvpnGroupCoreInterface>>

        Mockito.doNothing().`when`(underlayAccess).delete(Mockito.any())

        // test
        target.deleteCurrentAttributes(IID_CORE_IFC_CONFIG, data, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).delete(idCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<EvpnGroupCoreInterface>>
        )
    }
}