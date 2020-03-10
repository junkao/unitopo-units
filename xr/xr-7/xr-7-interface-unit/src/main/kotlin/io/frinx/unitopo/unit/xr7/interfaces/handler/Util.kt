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

package io.frinx.unitopo.unit.xr7.interfaces.handler

import com.google.common.base.Preconditions
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType
import java.util.regex.Pattern

class Util {

    companion object {

        const val ZERO_SUBINTERFACE_ID = 0L

        val SUBINTERFACE_NAME = Pattern.compile("(?<ifcId>.+)[.](?<subifcIndex>[0-9]+)")

        fun getSubIfcName(ifcName: String, subifcIdx: Long) = "$ifcName.$subifcIdx"

        fun parseIfcType(name: String): Class<out InterfaceType> {
            return when {
                name.startsWith("HundredGigE") -> EthernetCsmacd::class.java
                name.startsWith("TenGigE") -> EthernetCsmacd::class.java
                name.startsWith("GigabitEthernet") -> EthernetCsmacd::class.java
                name.startsWith("Bundle-Ether") -> Ieee8023adLag::class.java
                name.startsWith("Loopback") -> SoftwareLoopback::class.java
                else -> Other::class.java
            }
        }

        fun isSubinterface(name: String): Boolean {
            return SUBINTERFACE_NAME.matcher(name).matches()
        }

        fun getInterfaceName(name: String): String {
            val matcher = SUBINTERFACE_NAME.matcher(name)
            return when (matcher.matches()) {
                true -> matcher.group("ifcId")
                else -> name
            }
        }

        fun getSubinterfaceKey(name: String): SubinterfaceKey {
            val matcher = SUBINTERFACE_NAME.matcher(name)

            Preconditions.checkState(matcher.matches())
            return SubinterfaceKey(matcher.group("subifcIndex").toLong())
        }

        fun getDefaultIfcCfg(name: String): InterfaceConfiguration {
            return InterfaceConfigurationBuilder().apply {
                interfaceName = InterfaceName(name)
                isShutdown = null
            }.build()
        }

        fun filterInterface(data: InterfaceConfigurations?, ifcName: String): InterfaceConfiguration? =
            data?.let { interfaceConfigurations ->
                interfaceConfigurations.interfaceConfiguration.orEmpty()
                    .firstOrNull { it.interfaceName.value == ifcName }
            }
    }
}