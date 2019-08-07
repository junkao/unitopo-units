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

package io.frinx.unitopo.unit.xr623.interfaces.handler.holdtime

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.HoldTime
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey

class HoldTimeConfigReaderTest {
    @Mock
    private lateinit var readContext: ReadContext
    private lateinit var underlayAccess: UnderlayAccess
    private lateinit var target: HoldTimeConfigReader

    companion object {
        val NC_HELPER = NetconfAccessHelper("/data_holdtime.xml")
        val IF_NAME = "GigabitEthernet0/0/0/0"
        val IID_CONFIG = IIDs.INTERFACES
            .child(Interface::class.java, InterfaceKey(IF_NAME))
            .child(HoldTime::class.java)
            .child(Config::class.java)
        val UP_VALUE = 120000L
        val DOWN_VALUE = 0L
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NC_HELPER)
        target = HoldTimeConfigReader(underlayAccess)
    }

    @Test
    fun testReadCurrentAttributes() {
        val builder = ConfigBuilder()
        target.readCurrentAttributes(IID_CONFIG, builder, readContext)
        Assert.assertEquals(UP_VALUE, builder.up)
        Assert.assertEquals(DOWN_VALUE, builder.down)
    }
}