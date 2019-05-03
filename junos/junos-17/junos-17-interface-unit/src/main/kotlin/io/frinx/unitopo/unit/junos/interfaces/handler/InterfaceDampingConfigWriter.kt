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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.damping.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Damping as JunosDamping
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Damping.HalfLife as JunosHalfLife
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Damping.MaxSuppress as JunosMaxSuppress
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Damping.Reuse as JunosReuse
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Damping.Suppress as JunosSuppress
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.DampingBuilder as JunosDampingBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey as JunosInterfaceKey

class InterfaceDampingConfigWriter(private val underlayAccess: UnderlayAccess)
    : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (underlayId, underlayDamping) = getData(id, dataAfter)
        Preconditions.checkArgument(isSupportedForInterface(underlayId),
                "Write: Damping configuration is not supported for: %s", id)

        try {
            underlayAccess.put(underlayId, underlayDamping)
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
                "Delete: Damping configuration is not supported for: %s", id)

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
        val (underlayId, underlayDamping) = getData(id, dataAfter)
        Preconditions.checkArgument(isSupportedForInterface(underlayId),
                "Update: Damping configuration is not supported for: %s", id)

        try {
            if (underlayDamping.isEnable == null) {
                // Check if disabling damping
                // since enable is an empty leaf, it cannot be done with merge
                val (_, before) = getData(id, dataBefore)
                if (before.isEnable != null) {
                    val previousStateWithoutShut = JunosDampingBuilder(before).setEnable(null).build()
                    underlayAccess.put(underlayId, previousStateWithoutShut)
                }
            }
            underlayAccess.merge(underlayId, underlayDamping)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
        Pair<InstanceIdentifier<JunosDamping>, JunosDamping> {
        val (_, underlayId) = getUnderlayId(id)

        val damping = JunosDampingBuilder()
                .setEnable(parseEnabled(dataAfter.isEnabled))
                .setHalfLife(JunosHalfLife(dataAfter.halfLife))
                .setReuse(JunosReuse(dataAfter.reuse))
                .setSuppress(JunosSuppress(dataAfter.suppress))
                .setMaxSuppress(JunosMaxSuppress(dataAfter.maxSuppress))
                .build()

        return Pair(underlayId, damping)
    }

    private fun parseEnabled(isEnabled: Boolean): Boolean? {
        return when {
            isEnabled -> true
            else -> null
        }
    }

    private fun getUnderlayId(id: InstanceIdentifier<Config>): Pair<String, InstanceIdentifier<JunosDamping>> {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val underlayId = InterfaceReader.IFCS.child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
            .child(JunosDamping::class.java)

        return Pair(ifcName, underlayId)
    }

    private fun isSupportedForInterface(deviceId: InstanceIdentifier<JunosDamping>): Boolean {
        return when (Util.parseIfcType(deviceId.firstKeyOf(JunosInterface::class.java).name)) {
            Other::class.java -> false
            SoftwareLoopback::class.java -> false
            Ieee8023adLag::class.java -> false
            else -> true
        }
    }
}