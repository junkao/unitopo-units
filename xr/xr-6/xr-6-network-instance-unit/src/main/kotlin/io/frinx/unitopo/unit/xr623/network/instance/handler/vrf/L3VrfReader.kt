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

package io.frinx.unitopo.unit.xr623.network.instance.handler.vrf

import io.frinx.unitopo.ni.base.handler.vrf.AbstractL3VrfReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.Util
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr623.ospf.handler.OspfProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev161219.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev161219.vrf.groups.vrf.group.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey

class L3VrfReader(underlayAccess: UnderlayAccess) : AbstractL3VrfReader<Vrf>(underlayAccess) {

    override fun parseIds(): MutableList<NetworkInstanceKey> {
        // add vrf names defined in interface
        val list = underlayAccess.read(InterfaceReader.IFC_CFGS).checkedGet().orNull()
            ?.let {
                ifaceConfig -> ifaceConfig.interfaceConfiguration?.filter {
                    Util.isSubinterface(it.interfaceName.value)
                }?.mapNotNull {
                    it.getAugmentation(InterfaceConfiguration1::class.java)?.vrf
                }?.map {
                    it.value
                }
            }.orEmpty().toMutableList()

        underlayAccess.read(OspfProtocolReader.UNDERLAY_OSPF).checkedGet().orNull()
            ?.let {
                processes -> processes.process?.map {
                    process -> process?.vrfs?.vrf?.map {
                        list.add(it.vrfName.value)
                    }
                }
            }

        // get a distinct list
        return list.distinct().map {
            NetworkInstanceKey(it)
        }.toMutableList()
    }
}