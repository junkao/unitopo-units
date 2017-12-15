/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lr.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.unitopo.unit.xr6.lr.common.LrReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.Static
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.StaticBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.StaticKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.StateBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class StaticStateReader : LrReader.LrOperReader<State, StateBuilder> {

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<State>) = StateBuilder()

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributesForType(instanceIdentifier: InstanceIdentifier<State>, stateBuilder: StateBuilder, readContext: ReadContext) {
        val ipPrefix = instanceIdentifier.firstKeyOf<Static, StaticKey>(Static::class.java).prefix
        stateBuilder.prefix = ipPrefix
    }

    override fun merge(builder: Builder<out DataObject>, state: State) {
        (builder as StaticBuilder).state = state
    }
}