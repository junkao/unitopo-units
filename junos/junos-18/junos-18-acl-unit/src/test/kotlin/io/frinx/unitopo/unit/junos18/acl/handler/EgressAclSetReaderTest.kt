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

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.openconfig.acl.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.ACLIPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.ACLTYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.EgressAclSets
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.EgressAclSetsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.egress.acl.sets.EgressAclSet
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.egress.acl.sets.EgressAclSetBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526._interface.egress.acl.top.egress.acl.sets.EgressAclSetKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.acl.rev170526.acl.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId

class EgressAclSetReaderTest {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess: UnderlayAccess = NetconfAccessHelper("/data_nodes.xml")
    private lateinit var target: EgressAclSetReader

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        target = EgressAclSetReader(underlayAccess)
    }

    @Test
    fun testGetAllIds() {
        val ifId = "ae2220.46"
        val id = IIDs.AC_INTERFACES
                .child(Interface::class.java, InterfaceKey(InterfaceId(ifId)))
                .child(EgressAclSets::class.java)
                .child(EgressAclSet::class.java)

        val result = target.getAllIds(id, readContext)

        Assert.assertThat(result.size, CoreMatchers.`is`(1))
        Assert.assertThat(result[0].setName, CoreMatchers.equalTo("FBF-MS-THU-RETURN-OUTPUT"))
        Assert.assertThat(result[0].type, CoreMatchers.equalTo(ACLIPV4::class.java) as Matcher<Class<out ACLTYPE>>)
    }

    @Test
    fun testReadCurrentAttributes() {
        val ifId = "ae2220.46"
        val inetType = ACLIPV4::class.java
        val setName = "FBF-MS-THU-RETURN-OUTPUT"
        val egressAclSetKey = EgressAclSetKey(setName, inetType)
        val id = IIDs.AC_INTERFACES
                .child(Interface::class.java, InterfaceKey(InterfaceId(ifId)))
                .child(EgressAclSets::class.java)
                .child(EgressAclSet::class.java, egressAclSetKey)
        val builder = EgressAclSetBuilder()

        target.readCurrentAttributes(id, builder, readContext)

        Assert.assertThat(builder.key, CoreMatchers.sameInstance(egressAclSetKey))
        Assert.assertThat(builder.setName, CoreMatchers.sameInstance(setName))
        Assert.assertThat(builder.type, CoreMatchers.equalTo(inetType) as Matcher<Class<out ACLTYPE>>)
    }

    @Test
    fun testMerge() {
        val parentBuilder = EgressAclSetsBuilder()
        val data: List<EgressAclSet> = emptyList()

        target.merge(parentBuilder, data)

        Assert.assertThat(parentBuilder.egressAclSet, CoreMatchers.sameInstance(data))
    }

    @Test
    fun testGetBuilder() {
        val result = target.getBuilder(IIDs.AC_IN_IN_EG_EGRESSACLSET)

        Assert.assertThat(result, CoreMatchers.instanceOf(EgressAclSetBuilder::class.java))
    }
}