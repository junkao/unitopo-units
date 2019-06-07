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

package io.frinx.unitopo.unit.xr66.network.instance.handler.vrf.ifc

import io.frinx.unitopo.ni.base.handler.vrf.ifc.AbstractVrfInterfaceReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr66.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr66.interfaces.handler.Util
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev180615.InterfaceConfiguration1

class VrfInterfaceReader(underlayAccess: UnderlayAccess) : AbstractVrfInterfaceReader(underlayAccess) {

    override fun getAllInterfaces(vrfName: String): List<String> {
        val allIfcs = underlayAccess.read(InterfaceReader.IFC_CFGS)
                .checkedGet()
                .orNull()?.interfaceConfiguration.orEmpty()

        return allIfcs.filter {
                            it.getAugmentation(InterfaceConfiguration1::class.java)?.vrf
                            ?.value == vrfName && Util.isSubinterface(it.interfaceName.value)
                        }.map { it.interfaceName.value }
    }
}