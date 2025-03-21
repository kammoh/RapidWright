/*
 * Copyright (c) 2017-2022, Xilinx, Inc.
 * Copyright (c) 2022, Advanced Micro Devices, Inc.
 * All rights reserved.
 *
 * Author: Chris Lavin, Xilinx Research Labs.
 *
 * This file is part of RapidWright.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
/**
 *
 */
package com.xilinx.rapidwright.placer.handplacer;

import io.qt.core.QPointF;
import io.qt.widgets.QGraphicsPolygonItem;
import io.qt.gui.QPolygonF;

/**
 * Created on: Sep 16, 2015
 */
public class ValidPlacementPolygon extends QGraphicsPolygonItem {

    private QPointF anchorOffset;

    public ValidPlacementPolygon(QPolygonF polygon, QPointF anchorOffset) {
        setPolygon(polygon);
        this.anchorOffset = anchorOffset;
    }
}
