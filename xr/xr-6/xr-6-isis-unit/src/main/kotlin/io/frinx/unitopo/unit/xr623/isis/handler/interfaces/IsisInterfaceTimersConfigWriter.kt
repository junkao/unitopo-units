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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces._interface.LspRetransmitIntervals
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces._interface.lsp.retransmit.intervals.LspRetransmitInterval
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces._interface.lsp.retransmit.intervals.LspRetransmitIntervalBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces._interface.lsp.retransmit.intervals.LspRetransmitIntervalKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.IsisInternalLevel
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisIfTimersConfAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._interface.group.timers.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class IsisInterfaceTimersConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {
    override fun writeCurrentAttributes(id: IID<Config>, dataAfter: Config, wtx: WriteContext) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name

        require(vrfName == NetworInstance.DEFAULT_NETWORK_NAME) {
            "IS-IS configuration should be set in default network: $vrfName"
        }

        val (underlayId, builder) = getData(id, dataAfter)
        underlayAccess.safePut(underlayId, builder)
    }

    override fun updateCurrentAttributes(
        iid: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeCurrentAttributes(iid, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributes(id: IID<Config>, dataBefore: Config, wtx: WriteContext) {
        val instanceName = id.firstKeyOf(Protocol::class.java).name
        val interfaceId = id.firstKeyOf(Interface::class.java).interfaceId.value
        val underlayId = getUnderlayId(instanceName, interfaceId)

        underlayAccess.delete(underlayId)
    }

    private fun getData(id: IID<Config>, dataAfter: Config): Pair<IID<LspRetransmitInterval>, LspRetransmitInterval> {
        val instanceName = id.firstKeyOf(Protocol::class.java)
        val interfaceId = id.firstKeyOf(Interface::class.java)
        val underlayId = getUnderlayId(instanceName.name, interfaceId.interfaceId.value)

        val builder = LspRetransmitIntervalBuilder()
                .setLevel(IsisInternalLevel.NotSet)
                .setInterval(dataAfter.getAugmentation(IsisIfTimersConfAug::class.java)?.retransmissionInterval)

        return Pair(underlayId, builder.build())
    }

    companion object {
        fun getUnderlayId(instanceName: String, interfaceId: String): IID<LspRetransmitInterval> {
            return IsisInterfaceWriter.getUnderlayId(instanceName, interfaceId)
                .child(LspRetransmitIntervals::class.java)
                .child(LspRetransmitInterval::class.java, LspRetransmitIntervalKey(IsisInternalLevel.NotSet))
        }
    }
}