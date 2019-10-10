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
package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc

import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.ConfigBuilder

class SubinterfaceConfigReaderTest : AbstractNetconfHandlerTest() {

    private var data: InterfaceConfigurations = parseGetCfgResponse(getResourceAsString("/data_ifaces.xml"),
        InterfaceReader.IFC_CFGS)

    private var reader: SubinterfaceConfigReader = SubinterfaceConfigReader(Mockito.mock(UnderlayAccess::class.java))

    @Test
    fun testReadCurrentAttributes() {
        val configBuilder = ConfigBuilder()
        reader.readData(data, configBuilder, "GigabitEthernet0/0/0/0", 44)
        Assert.assertEquals("bl", configBuilder.description)
        Assert.assertEquals("GigabitEthernet0/0/0/0.44", configBuilder.name)
        Assert.assertEquals(44, configBuilder.index)
        Assert.assertFalse(configBuilder.isEnabled)
    }
}