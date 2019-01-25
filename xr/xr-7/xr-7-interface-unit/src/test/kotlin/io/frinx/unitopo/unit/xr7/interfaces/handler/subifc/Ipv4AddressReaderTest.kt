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

package io.frinx.unitopo.unit.xr7.interfaces.handler.subifc

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.openconfig.openconfig._if.ip.IIDs as IfIpIIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.Ipv4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.Addresses
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.AddressesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.Subinterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone

class Ipv4AddressReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private lateinit var target: Ipv4AddressReader

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        target = Ipv4AddressReader(underlayAccess)
    }

    @Test
    fun testGetAllIds() {
        val ifName = "GigabitEthernet0/0/0/0"
        val ipAddress = "10.1.5.28"

        val id = IIDs.INTERFACES
                .child(Interface::class.java, InterfaceKey(ifName))
                .child(Subinterfaces::class.java)
                .child(Subinterface::class.java, SubinterfaceKey(0L))
                .augmentation(Subinterface1::class.java)
                .child(Ipv4::class.java)
                .child(Addresses::class.java)
                .child(Address::class.java, AddressKey(Ipv4AddressNoZone(ipAddress)))

        val result = target.getAllIds(id, readContext)

        Assert.assertThat(
                result.map { it.ip },
                Matchers.containsInAnyOrder(Ipv4AddressNoZone(ipAddress))
        )
    }

    @Test
    fun testReadCurrentAttributes() {
        val ifName = "GigabitEthernet0/0/0/0"
        val ipAddress = "10.1.5.28"

        val id = IIDs.INTERFACES
                .child(Interface::class.java, InterfaceKey(ifName))
                .child(Subinterfaces::class.java)
                .child(Subinterface::class.java, SubinterfaceKey(0L))
                .augmentation(Subinterface1::class.java)
                .child(Ipv4::class.java)
                .child(Addresses::class.java)
                .child(Address::class.java, AddressKey(Ipv4AddressNoZone(ipAddress)))

        val builder = AddressBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.ip, CoreMatchers.equalTo(Ipv4AddressNoZone(ipAddress)))
    }

    @Test
    fun testMerge() {
        val list: List<Address> = emptyList()
        val data: MutableList<Address> = list.toMutableList()
        val parentBuilder = AddressesBuilder()

        target.merge(parentBuilder, data)

        Assert.assertThat(parentBuilder.`address`, CoreMatchers.sameInstance(data))
    }

    @Test
    fun testGetBuilder() {
        val result = target.getBuilder(IfIpIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_ADDRESS)

        Assert.assertThat(result, CoreMatchers.instanceOf(AddressBuilder::class.java))
    }
}