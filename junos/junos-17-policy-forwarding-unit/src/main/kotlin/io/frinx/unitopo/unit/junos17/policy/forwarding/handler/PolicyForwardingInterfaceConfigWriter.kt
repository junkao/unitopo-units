/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos17.policy.forwarding.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.juniper.rev171109.NiPfIfJuniperAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.cos_interfaces_type.Unit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.cos_interfaces_type.UnitBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.cos_interfaces_type.unit.ClassifiersBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.cos_interfaces_type.unit.classifiers.Exp
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.cos_interfaces_type.unit.classifiers.InetPrecedenceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.cos_interfaces_type.unit.classifiers.ExpBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.cos_interfaces_type.unit.classifiers.InetPrecedence
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.Interface as OcInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper._class.of.service.options.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper._class.of.service.options.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper._class.of.service.options.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class PolicyForwardingInterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, data: Config, p2: WriteContext) {
        val (underlayId, underlayIfcCfg) = getData(data)
        try {
            underlayAccess.put(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, p1: Config, p2: WriteContext) {
        val ifcName = id.firstKeyOf(OcInterface::class.java).interfaceId.value
        val underlayId = getId(ifcName)
        try {
            underlayAccess.delete(underlayId)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(data : Config):
            Pair<InstanceIdentifier<Interface>, Interface> {
        val underlayIfcCfg = InterfaceBuilder().setName(data.interfaceId.value)
        data.getAugmentation(NiPfIfJuniperAug::class.java)?.let {
            underlayIfcCfg.schedulerMap = it.schedulerMap
            val cBuilder = ClassifiersBuilder()
            cBuilder.inetPrecedence = InetPrecedenceBuilder().setClassifierName(InetPrecedence.ClassifierName(it.classifiers?.inetPrecedence?.name)).build()
            cBuilder.exp = ExpBuilder().setClassifierName(Exp.ClassifierName(it.classifiers?.exp?.name)).build()
            val uBuilder = UnitBuilder().setName(Unit.Name("0"))
                    .setClassifiers(cBuilder.build())
            underlayIfcCfg.unit = listOf(uBuilder.build())
        }
        return Pair(getId(data.interfaceId.value), underlayIfcCfg.build())
    }

    private fun getId(ifcName: String): InstanceIdentifier<Interface> =
            PolicyForwardingInterfaceReader.CLASS_OF_SERVICE.child(Interface::class.java, InterfaceKey(ifcName))

}