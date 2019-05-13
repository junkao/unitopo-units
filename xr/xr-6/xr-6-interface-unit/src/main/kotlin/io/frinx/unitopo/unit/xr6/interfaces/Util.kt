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

package io.frinx.unitopo.unit.xr6.interfaces

import com.google.common.base.Preconditions
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType
import java.util.regex.Pattern

class Util {

    companion object {

        private val SUBINTERFACE_NAME = Pattern.compile("(?<ifcId>.+)[.](?<subifcIndex>[0-9]+)")

        fun parseIfcType(name: String): Class<out InterfaceType> {
            return when {
                name.startsWith("MgmtEth") -> EthernetCsmacd::class.java
                name.startsWith("FastEther") -> EthernetCsmacd::class.java
                name.startsWith("GigabitEthernet") -> EthernetCsmacd::class.java
                name.startsWith("Loopback") -> SoftwareLoopback::class.java
                name.startsWith("Bundle") -> Ieee8023adLag::class.java
                else -> Other::class.java
            }
        }

        fun isSubinterface(name: String): Boolean {
            return SUBINTERFACE_NAME.matcher(name).matches()
        }

        fun getSubinterfaceKey(name: String): SubinterfaceKey {
            val matcher = SUBINTERFACE_NAME.matcher(name)

            Preconditions.checkState(matcher.matches())
            return SubinterfaceKey(matcher.group("subifcIndex").toLong())
        }
    }
}