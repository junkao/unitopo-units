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

package io.frinx.unitopo.unit.xr7.interfaces.handler.subifc.holdtime

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.cisco.rev171024.IfSubifCiscoHoldTimeAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.HoldTime
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.Subinterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey

class HoldTimeConfigReaderTest {
    @Mock
    private lateinit var readContext: ReadContext
    private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private lateinit var target: HoldTimeConfigReader

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        target = HoldTimeConfigReader(underlayAccess)
    }

    @Test
    fun readCurrentAttributes() {
        val builder = ConfigBuilder()

        target.readCurrentAttributes(IID, builder, readContext)

        Assert.assertThat(builder.down, CoreMatchers.nullValue())
        Assert.assertThat(builder.up, CoreMatchers.`is`(100L))
    }

    companion object {
        private val INTERFACE_NAME = "Bundle-Ether4001"
        private val SUBINTERFACE_INDEX = 10L

        private val IID = IIDs.INTERFACES
                .child(Interface::class.java, InterfaceKey(INTERFACE_NAME))
                .child(Subinterfaces::class.java)
                .child(Subinterface::class.java, SubinterfaceKey(SUBINTERFACE_INDEX))
                .augmentation(IfSubifCiscoHoldTimeAug::class.java)
                .child(HoldTime::class.java)
                .child(Config::class.java)
    }
}