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

package io.frinx.unitopo.unit.junos.interfaces.handler

import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.HoldTime as JunosHoldTime
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.HoldTime.Down as JunosHoldTimeDown
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.HoldTime.Up as JunosHoldTimeUp
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.HoldTimeBuilder as JunosHoldTimeBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey as JunosInterfaceKey

class InterfaceHoldTimeConfigWriter(private val underlayAccess: UnderlayAccess) :
    WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (underlayId, underlayHoldTime) = getData(id, dataAfter)
        Preconditions.checkArgument(isSupportedForInterface(underlayId),
                "Write: HoldTime configuration is not supported for: %s", id)

        try {
            underlayAccess.put(underlayId, underlayHoldTime)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val (_, underlayId) = getUnderlayId(id)
        Preconditions.checkArgument(isSupportedForInterface(underlayId),
                "Delete: HoldTime configuration is not supported for: %s", id)

        try {
            underlayAccess.delete(underlayId)
        } catch (e: Exception) {
            throw ReadFailedException(id, e)
        }
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (underlayId, underlayHoldTime) = getData(id, dataAfter)
        Preconditions.checkArgument(isSupportedForInterface(underlayId),
                "Update: HoldTime configuration is not supported for: %s", id)

        try {
            underlayAccess.merge(underlayId, underlayHoldTime)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
        Pair<InstanceIdentifier<JunosHoldTime>, JunosHoldTime> {
        val (_, underlayId) = getUnderlayId(id)

        val holdTime = JunosHoldTimeBuilder()
                .setUp((JunosHoldTimeUp(dataAfter.up)))
                .setDown(JunosHoldTimeDown(dataAfter.down))
                .build()

        return Pair(underlayId, holdTime)
    }

    private fun getUnderlayId(id: InstanceIdentifier<Config>): Pair<String, InstanceIdentifier<JunosHoldTime>> {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val underlayId = InterfaceReader.IFCS
                .child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
                .child(JunosHoldTime::class.java)

        return Pair(ifcName, underlayId)
    }

    private fun isSupportedForInterface(deviceId: InstanceIdentifier<JunosHoldTime>): Boolean {
        return when (parseIfcType(deviceId.firstKeyOf(JunosInterface::class.java).name)) {
            Ieee8023adLag::class.java -> false
            else -> true
        }
    }
}