/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.init

import io.frinx.unitopo.registry.spi.TranslateUnit

/**
 * This unit is a holder of generic settings for XR6 netconf session. No handlers defined here.
 */
abstract class Unit : TranslateUnit {

    override fun toString(): String = "XR 6 init unit"

    override fun useAutoCommit() = true

}


