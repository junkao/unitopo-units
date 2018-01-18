/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos17.network.instance

import com.google.common.collect.Lists
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.frinx.cli.registry.common.CompositeWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.network.instance.common.def.DefaultConfigWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config

class NetworkInstanceConfigWriter(access: UnderlayAccess) :
    CompositeWriter<Config>(Lists.newArrayList<WriterCustomizer<Config>>(
        DefaultConfigWriter())
    )
