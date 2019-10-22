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

package io.frinx.unitopo.unit.xr6.bgp.handler.neighbor

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.common.structure.neighbor.group.ebgp.multihop.ebgp.multihop.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv4Address

class NeighborEbgpMultihopConfigReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/bgp-conf4.xml")

    @Test
    fun parseEbgpMultihopTest() {
        val builder = ConfigBuilder()

        NeighborEbgpMultihopConfigReader.parseEbgpMultihop(parseGetCfgResponse(DATA_NODES,
                BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java,
                        InstanceKey(CiscoIosXrString("default")))),
                NeighborKey(IpAddress(Ipv4Address("99.0.0.99"))), builder)

        Assert.assertEquals(ConfigBuilder().setEnabled(true).setMultihopTtl(1).build(), builder.build())
    }
}