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

package io.frinx.unitopo.unit.junos.interfaces.handler

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType

class Util {

    companion object {
        fun parseIfcType(name: String): Class<out InterfaceType>? {
            return when {
                name.startsWith("em") -> EthernetCsmacd::class.java
                name.startsWith("et") -> EthernetCsmacd::class.java
                name.startsWith("fe") -> EthernetCsmacd::class.java
                name.startsWith("fxp") -> EthernetCsmacd::class.java
                name.startsWith("ge") -> EthernetCsmacd::class.java
                name.startsWith("xe") -> EthernetCsmacd::class.java
                name.startsWith("lo") -> SoftwareLoopback::class.java
                name.startsWith("ae") -> Ieee8023adLag::class.java
                else -> Other::class.java
            }
        }
    }
}