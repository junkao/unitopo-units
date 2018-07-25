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
package io.frinx.unitopo.unit.xr6.ospf.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.handlers.ospf.OspfWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.Ospf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.Processes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.ProcessBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.ProcessKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.Config
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfProtocolWriter(private val underlayAccess: UnderlayAccess) : OspfWriter<Config> {

    override fun updateCurrentAttributesForType(
        iid: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        deleteCurrentAttributesForType(iid, dataBefore, writeContext)
        writeCurrentAttributesForType(iid, dataAfter, writeContext)
    }

    override fun writeCurrentAttributesForType(id: IID<Config>, dataAfter: Config, wtx: WriteContext) {
        val (underlayId, underlayCfg) = getData(id)

        underlayAccess.merge(underlayId, underlayCfg)
    }

    override fun deleteCurrentAttributesForType(id: IID<Config>, dataBefore: Config, wtx: WriteContext) {
        val processName = id.firstKeyOf(Protocol::class.java).name
        val processId = IID.create(Ospf::class.java).child(Processes::class.java).child(Process::class.java,
                ProcessKey(CiscoIosXrString(processName)))

        underlayAccess.delete(processId)
    }

    private fun getData(id: IID<Config>):
            Pair<IID<Process>, Process> {
        val processName = id.firstKeyOf(Protocol::class.java).name
        val processIdentifier = getIdentifiers(id)
        val builder = ProcessBuilder()
                .setKey(ProcessKey(CiscoIosXrString(processName)))
                .setStart(true)

        return Pair(processIdentifier, builder.build())
    }

    companion object {
        fun getIdentifiers(id: IID<out DataObject>): IID<Process> {
            val processName = id.firstKeyOf(Protocol::class.java).name
            return IID.create(Ospf::class.java).child(Processes::class.java).child(Process::class.java,
                    ProcessKey(CiscoIosXrString(processName)))
        }
    }
}