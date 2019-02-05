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

package io.frinx.unitopo.unit.xr7.bgp.handler

import com.google.common.base.Optional
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.Bgp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.InstanceAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.GlobalAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.global.afs.GlobalAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.global.afs.GlobalAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev170626.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.afi.safi.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.afi.safi.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.AfiSafis
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.L2VPNEVPN
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.ConfigBuilder as BgpConfigBuilder
class GlobalAfiSafiConfigWriterTest : AbstractNetconfHandlerTest() {

    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: GlobalAfiSafiConfigWriter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(GlobalAfiSafiConfigWriterTest.DATA_NODES))
        target = Mockito.spy(GlobalAfiSafiConfigWriter(underlayAccess))
    }

    companion object {

        private val DATA_NODES = NetconfAccessHelper("/bgp-conf4.xml")

        private val IID_IFNAME = "default"
        private val IID_PROTOCOL = "default"
        private val NATIV_AFNAME = "l2vpn-evpn"
        private val NATIV_IID: InstanceIdentifier<GlobalAf> = InstanceIdentifier
                .create(Bgp::class.java)
                .child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))
                .child(InstanceAs::class.java)
                .child(FourByteAs::class.java)
                .child(DefaultVrf::class.java)
                .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp
                        .instance.instance
                        .`as`.four._byte.`as`._default.vrf.Global::class.java)
                .child(GlobalAfs::class.java)
                .child(GlobalAf::class.java)

        private val IID_CONFIG = KeyedInstanceIdentifier
                .create(NetworkInstances::class.java)
                .child(NetworkInstance::class.java, NetworkInstanceKey(IID_IFNAME))
                .child(Protocols::class.java)
                .child(Protocol::class.java, ProtocolKey(BGP::class.java, IID_PROTOCOL))
                .child(org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202
                        .bgp.top.Bgp::class.java)
                .child(org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202
                        .bgp.top.bgp.Global::class.java)
                .child(AfiSafis::class.java)
                .child(AfiSafi::class.java, AfiSafiKey(L2VPNEVPN::class.java))
                .child(Config::class.java)

        private val CONFIG = ConfigBuilder()
                .setAfiSafiName(L2VPNEVPN::class.java)
                .setEnabled(true)
                .build()
        private val NATIVE_CONFIG = GlobalAfBuilder()
                .setAfName(BgpAddressFamily.L2vpnEvpn)
                .setEnable(true)
                .build()
    }
    @Test
    fun testWriteCurrentAttributes() {

        val config = ConfigBuilder(CONFIG)
                .build()

        val idCap = ArgumentCaptor
                .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<GlobalAf>>

        val expectedConfig = GlobalAfBuilder(NATIVE_CONFIG) // not customize
                .build()

        val dataCap = ArgumentCaptor
                .forClass(DataObject::class.java) as ArgumentCaptor<GlobalAf>

        val globalBuilder = GlobalBuilder().apply {

            this.config = BgpConfigBuilder().apply {
                this.`as` = AsNumber(19999L)
            }.build()
        }
        Mockito.doNothing().`when`(underlayAccess).merge(Mockito.any(), Mockito.any())
        Mockito.`when`(writeContext?.readAfter(RWUtils.cutId(IID_CONFIG,
                org.opendaylight.yang.gen.v1.http.frinx.openconfig
                        .net.yang.bgp.rev170202.bgp.top.bgp.Global::class.java)))
                .thenReturn(Optional.of(globalBuilder.build()))

        target!!.writeCurrentAttributesForType(IID_CONFIG, config, writeContext!!)

        Mockito.verify(underlayAccess, Mockito.times(1)).merge(idCap.capture(), dataCap.capture())
        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(
                dataCap.allValues.get(0),
                CoreMatchers.equalTo(expectedConfig)
        )
    }
}