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

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.IsisInternalLevel
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.hello.authentication.group.key.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.hello.authentication.group.key.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.EncryptedPasswordBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.EncryptedString
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class IsisInterfaceAuthConfigReader(private val access: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun readCurrentAttributes(id: IID<Config>, config: ConfigBuilder, readContext: ReadContext) {
        val protKey = id.firstKeyOf(Protocol::class.java)
        val ifaceId = id.firstKeyOf(Interface::class.java).interfaceId

        IsisInterfaceReader.getInterfaces(access, protKey)
                ?.`interface`.orEmpty()
                .find { it.interfaceName.value == ifaceId.value }
                ?.let {
                    it.helloPasswords?.helloPassword.orEmpty()
                            .find { it.level == IsisInternalLevel.NotSet }
                            ?.let {
                                config.authPassword = EncryptedPasswordBuilder.getEncryptedPassword(it.password.value)
                            }
                }
    }

    companion object {
        private val PASSWORD_ENCRYPTED_PATTERN = EncryptedString.PATTERN_CONSTANTS[0]
    }
}