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

package io.frinx.unitopo.unit.xr7.logging.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.logging.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.interfaces.structural.InterfacesBuilder

class LoggingInterfacesReaderTest : AbstractNetconfHandlerTest() {

    @Mock
    private lateinit var readContext: ReadContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: LoggingInterfacesReader

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        private val NC_ALL_DISABLE = NetconfAccessHelper("/data_nodes_all_disable.xml")
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = LoggingInterfacesReader(underlayAccess)
    }

    @Test
    fun testReadCurrentAttributesNormal() {
        val ifName = "Bundle-Ether301"
        val interfacesBuilder = InterfacesBuilder()

        target.readCurrentAttributes(IIDs.LO_INTERFACES, interfacesBuilder, readContext)

        Assert.assertThat(interfacesBuilder.`interface`[0].interfaceId.value, CoreMatchers.equalTo(ifName))
    }

    @Test
    fun testReadCurrentAttributesAllDisable() {
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_ALL_DISABLE))
        target = LoggingInterfacesReader(underlayAccess)

        val interfacesBuilder = InterfacesBuilder()

        target.readCurrentAttributes(IIDs.LO_INTERFACES, interfacesBuilder, readContext)

        Assert.assertThat(interfacesBuilder.`interface`, CoreMatchers.nullValue())
    }
}