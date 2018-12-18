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

package io.frinx.unitopo.unit.junos18.acl.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.acl.IIDs
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId

class AclInterfaceWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext

    private val target = AclInterfaceWriter()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testWriteCurrentAttributes_Normal() {
        val ifId = "ae2220.46"
        val id = IIDs.AC_INTERFACES.child(Interface::class.java, InterfaceKey(InterfaceId(ifId)))
        val dataAfter: Interface = Mockito.mock(Interface::class.java)

        target.writeCurrentAttributes(id, dataAfter, writeContext)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testWriteCurrentAttributes_FormatError() {
        val ifId = "ae2220"
        val id = IIDs.AC_INTERFACES.child(Interface::class.java, InterfaceKey(InterfaceId(ifId)))
        val dataAfter: Interface = Mockito.mock(Interface::class.java)

        target.writeCurrentAttributes(id, dataAfter, writeContext)
    }
}