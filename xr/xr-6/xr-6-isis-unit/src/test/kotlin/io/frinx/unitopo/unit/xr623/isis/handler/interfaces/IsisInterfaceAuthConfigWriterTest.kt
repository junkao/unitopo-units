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

package io.frinx.unitopo.unit.xr623.isis.handler.interfaces

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.IsisAuthenticationAlgorithm
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.IsisAuthenticationFailureMode
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces._interface.HelloPasswords
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces._interface.hello.passwords.HelloPassword
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces._interface.hello.passwords.HelloPasswordKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.IsisInternalLevel
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.hello.authentication.group.key.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.hello.authentication.group.key.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.EncryptedPassword
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.EncryptedString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.ISIS
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.utils.IidUtils
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.Isis as XrIsis
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.Instances as XrInstances
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.Instance as XrInstance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.InstanceKey as XrInstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.Interfaces as XrInterfaces
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces.Interface as XrInterface
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces.InterfaceKey as XrInterfaceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.IsisInstanceName as XrIsisInstanceName
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName as XrInterfaceName
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class IsisInterfaceAuthConfigWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext
    private lateinit var underlayAccess: UnderlayAccess
    private lateinit var target: IsisInterfaceAuthConfigWriter

    private lateinit var idCap: ArgumentCaptor<IID<DataObject>>
    private lateinit var dataCap: ArgumentCaptor<HelloPassword>

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")

        private val INSTANCE_NAME = "ISIS-001"
        private val INTERFACE_ID = "Bundle-Ether4001"
        private val PASSWORD_HASH = "1234567890"
        private val ENCRYPTED_PASSWORD = "Encrypted[$PASSWORD_HASH]"

        private val IID_CONFIG = IidUtils.createIid(IIDs.NE_NE_PR_PR_IS_IN_IN_AU_KE_CONFIG,
                NetworInstance.DEFAULT_NETWORK,
                ProtocolKey(ISIS::class.java, INSTANCE_NAME),
                InterfaceKey(InterfaceId(INTERFACE_ID))) as IID<Config>

        private val CONFIG = ConfigBuilder()
                .setAuthPassword(EncryptedPassword(EncryptedString(ENCRYPTED_PASSWORD)))
                .build()

        private val NATIVE_IID = IID
                .create(XrIsis::class.java)
                .child(XrInstances::class.java)
                .child(XrInstance::class.java, XrInstanceKey(XrIsisInstanceName(INSTANCE_NAME)))
                .child(XrInterfaces::class.java)
                .child(XrInterface::class.java, XrInterfaceKey(XrInterfaceName(INTERFACE_ID)))
                .child(HelloPasswords::class.java)
                .child(HelloPassword::class.java, HelloPasswordKey(IsisInternalLevel.NotSet))
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NetconfAccessHelper(NC_HELPER))
        target = IsisInterfaceAuthConfigWriter(underlayAccess)

        idCap = ArgumentCaptor.forClass(IID::class.java) as ArgumentCaptor<IID<DataObject>>
        dataCap = ArgumentCaptor.forClass(HelloPassword::class.java)
    }

    @Test
    fun testWriteCurrentAttributesForType() {
        val config = ConfigBuilder(CONFIG)
                .build()

        Mockito.doNothing().`when`(underlayAccess).put(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(IID_CONFIG, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).put(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
                idCap.allValues[0],
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<DataObject>>
        )
        Assert.assertThat(dataCap.allValues[0].password.value, CoreMatchers.equalTo(PASSWORD_HASH))
        Assert.assertThat(dataCap.allValues[0].level, CoreMatchers.equalTo(IsisInternalLevel.NotSet))
        Assert.assertThat(dataCap.allValues[0].algorithm, CoreMatchers.equalTo(IsisAuthenticationAlgorithm.HmacMd5))
        Assert.assertThat(dataCap.allValues[0].failureMode, CoreMatchers.equalTo(IsisAuthenticationFailureMode.Drop))
    }

    @Test
    fun testDeleteCurrentAttributesForType() {
        val config = ConfigBuilder(CONFIG)
                .build()

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
                CoreMatchers.equalTo(NATIVE_IID) as Matcher<IID<DataObject>>
        )
    }
}