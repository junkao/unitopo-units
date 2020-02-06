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

package io.frinx.unitopo.unit.xr7.ospf.handler

import com.google.common.base.Optional
import com.google.common.util.concurrent.CheckedFuture
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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.Ospf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.area.table.AreaAddresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.area.table.area.addresses.AreaAreaId
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.area.table.area.addresses.AreaAreaIdKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.area.table.area.addresses.area.content.NameScopes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.area.table.area.addresses.area.content.name.scopes.NameScope
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.area.table.area.addresses.area.content.name.scopes.NameScopeKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.ospf.Processes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.ospf.processes.ProcessKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.ospf.processes.process.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.ospf.processes.process.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev191031.ospf.processes.process.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.types.rev170228.OspfAreaIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.Interfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.Ospfv2
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.Areas
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.AreaKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.OSPF
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier

class AreaInterfaceConfigWriterTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var writeContext: WriteContext
    @Mock
    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: AreaInterfaceConfigWriter

    companion object {
        private val DATA_NODES = getResourceAsString(javaClass, "/area_interfaces_nodes.xml")

        private val areaId: Long = 65535
        private val interfaceId: String = "HundredGigE0/0/0/0.65535"
        private val processId: String = "400"
        private val vrfName: String = "iups"

        private val IID_CONFIG = KeyedInstanceIdentifier
                .create(NetworkInstances::class.java)
                .child(NetworkInstance::class.java, NetworkInstanceKey(vrfName))
                .child(Protocols::class.java)
                .child(Protocol::class.java, ProtocolKey(OSPF::class.java, processId))
                .child(Ospfv2::class.java)
                .child(Areas::class.java)
                .child(Area::class.java, AreaKey(OspfAreaIdentifier(areaId)))
                .child(Interfaces::class.java)
                .child(Interface::class.java, InterfaceKey(interfaceId))
                .child(Config::class.java)

        private val CONFIG = ConfigBuilder().setId(interfaceId).build()

        private val NATIVE_IID = KeyedInstanceIdentifier
                .create(Ospf::class.java)
                .child(Processes::class.java)
                .child(Process::class.java, ProcessKey(CiscoIosXrString(processId)))
                .child(Vrfs::class.java)
                .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfName)))
                .child(AreaAddresses::class.java)
                .child(AreaAreaId::class.java, AreaAreaIdKey(areaId))
                .child(NameScopes::class.java)
                .child(NameScope::class.java, NameScopeKey(InterfaceName(interfaceId)))
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        target = Mockito.spy(AreaInterfaceConfigWriter(underlayAccess))
    }

    @Test
    fun testWriteCurrentAttributes() {
        val config = ConfigBuilder(CONFIG) // not customize
                .build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<NameScope>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<NameScope>

        Mockito.doNothing().`when`(underlayAccess).merge(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(IID_CONFIG, config, writeContext!!)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).merge(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<NameScope>>
        )
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val config = ConfigBuilder(CONFIG) // not customize
                .build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<NameScope>>

        Mockito.doNothing().`when`(underlayAccess).delete(Mockito.any())

        // test
        target.deleteCurrentAttributes(IID_CONFIG, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).delete(idCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<NameScope>>
        )
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val configBefore = ConfigBuilder(CONFIG) // not customize
                .build()
        val configAfter = ConfigBuilder(CONFIG).setId("HundredGigE0/0/0/0.10000")
                .build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<NameScope>>
        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<NameScope>
        val checkedFuture = Mockito.mock(CheckedFuture::class.java)

        val data = parseGetCfgResponse(DATA_NODES, NATIVE_IID)
        Mockito.doReturn(checkedFuture).`when`(underlayAccess).read(NATIVE_IID)
        Mockito.doReturn(Optional.of(data)).`when`(checkedFuture).checkedGet()
        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.updateCurrentAttributes(IID_CONFIG, configBefore, configAfter, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<NameScope>>
        )
    }
}