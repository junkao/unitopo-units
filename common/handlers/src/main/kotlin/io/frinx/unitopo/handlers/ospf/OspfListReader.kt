/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.handlers.ospf

import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.fd.honeycomb.translate.spi.read.OperListReaderCustomizer
import io.frinx.cli.registry.common.TypedListReader
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.Identifiable
import org.opendaylight.yangtools.yang.binding.Identifier

interface OspfListReader<O, K, B> : OspfReader<O, B>, TypedListReader<O, K, B>
        where O : DataObject, O : Identifiable<K>, K : Identifier<O>, B : Builder<O> {

    interface OspfConfigListReader<O : DataObject, K : Identifier<O>, B : Builder<O>> : OspfListReader<O, K, B>, ConfigListReaderCustomizer<O, K, B> where O : Identifiable<K>

    interface OspfOperListReader<O : DataObject, K : Identifier<O>, B : Builder<O>>: OspfListReader<O, K, B>, OperListReaderCustomizer<O, K, B> where O : Identifiable<K>
}
