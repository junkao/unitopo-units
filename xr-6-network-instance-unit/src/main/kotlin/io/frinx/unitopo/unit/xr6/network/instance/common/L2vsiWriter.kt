/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.common

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.util.RWUtils
import io.frinx.cli.registry.common.TypedWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.*
import java.util.function.Function

interface L2vsiWriter<O : DataObject> : TypedWriter<O>, WriterCustomizer<O> {

    override fun getParentCheck(id: InstanceIdentifier<O>?) =
            AbstractMap.SimpleEntry<InstanceIdentifier<out DataObject>, Function<DataObject, Boolean>>(
                    RWUtils.cutId(id!!, NetworkInstance::class.java).child(Config::class.java),
                    L2vsiReader.L2VSI_CHECK)
}
