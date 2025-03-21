/*
 * Original work: Copyright (c) 2010-2011 Brigham Young University
 * Modified work: Copyright (c) 2017-2022, Xilinx, Inc.
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
package com.xilinx.rapidwright.device.browser;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import io.qt.core.QEvent;
import io.qt.core.Qt.MouseButton;
import io.qt.core.Qt.PenStyle;
import io.qt.gui.QAction;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.widgets.QGraphicsLineItem;
import io.qt.widgets.QGraphicsSceneMouseEvent;
import io.qt.widgets.QMenu;
import io.qt.gui.QPen;

import com.xilinx.rapidwright.design.tools.Edge;
import com.xilinx.rapidwright.design.tools.TileGroup;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Tile;
import com.xilinx.rapidwright.device.Wire;
import com.xilinx.rapidwright.gui.NumberedHighlightedTile;
import com.xilinx.rapidwright.gui.TileScene;
import com.xilinx.rapidwright.router.RouteNode;

import io.qt.core.Qt;

/**
 * This class was written specifically for the DeviceBrowser class.  It
 * provides the scene content of the 2D tile array.
 */
public class DeviceBrowserScene extends TileScene{
    /**     */
    public final Signal1<Tile> updateTile = new Signal1<>();
    /**     */
    private QPen wirePen;
    /**     */
    private ArrayList<QGraphicsLineItem> currLines;
    /**     */
    private DeviceBrowser browser;
    /**     */
    private Tile reachabilityTile;
    /**     */
    private ArrayList<NumberedHighlightedTile> currentTiles = new ArrayList<NumberedHighlightedTile>();

    // User Defined Qt Events
    public static final int CLEAR_HIGHLIGHTED_TILES = 1001;
    public static final int HIGHLIGHT_TILE_GROUPS = 1002;
    // End Events

    public DeviceBrowserScene(Device device, boolean hideTiles, boolean drawPrimitives, DeviceBrowser browser) {
        super(device, hideTiles, drawPrimitives);
        currLines = new ArrayList<QGraphicsLineItem>();
        wirePen = new QPen(Qt.GlobalColor.yellow, 0.25, PenStyle.SolidLine);
        this.browser = browser;
        QEvent.registerEventType(CLEAR_HIGHLIGHTED_TILES);
        QEvent.registerEventType(HIGHLIGHT_TILE_GROUPS);
    }

    public void drawWire(Tile src, Tile dst) {
        QGraphicsLineItem line = new QGraphicsLineItem(
                src.getColumn()*tileSize  + tileSize/2,
                src.getRow()*tileSize + tileSize/2,
                dst.getColumn()*tileSize + tileSize/2,
                dst.getRow()*tileSize + tileSize/2);
        line.setPen(wirePen);
        addItem(line);
    }

    public void clearCurrentLines() {
        for (QGraphicsLineItem line : currLines) {
            this.removeItem(line);
            line.dispose();
        }
        currLines.clear();
    }

    public void drawWire(Tile src, int wireSrc, Tile dst, int wireDst) {
        double enumSize = src.getWireCount();
        double x1 = (double) tileXMap.get(src)*tileSize  + (wireSrc%tileSize);
        double y1 = (double) tileYMap.get(src)*tileSize  + (wireSrc*tileSize)/enumSize;
        double x2 = (double) tileXMap.get(dst)*tileSize  + (wireDst%tileSize);
        double y2 = (double) tileYMap.get(dst)*tileSize  + (wireDst*tileSize)/enumSize;
        WireConnectionLine line = new WireConnectionLine(x1,y1,x2,y2, this, dst, wireDst);
        line.setToolTip(src.getName() + " " + src.getWireName(wireSrc) + " -> " +
                dst.getName() + " " + dst.getWireName(wireDst));
        line.setPen(wirePen);
        line.setAcceptHoverEvents(true);
        addItem(line);
        currLines.add(line);
    }

    public void drawConnectingWires(Tile tile, int wire) {
        clearCurrentLines();
        if (tile == null) return;
        for (Wire w : tile.getWireConnections(wire)) {
            drawWire(tile, wire, w.getTile(), w.getWireIndex());
        }
    }

    private HashMap<Tile, Integer> findReachability(Tile t, Integer hops) {
        HashMap<Tile, Integer> reachabilityMap = new HashMap<Tile, Integer>();

        Queue<RouteNode> queue = new LinkedList<RouteNode>();
        for (int wire = 0; wire < t.getWireCount(); wire++) {
            List<Wire> connections = t.getWireConnections(wire);
            if (connections == null) continue;
            for (Wire wc : connections) {
                queue.add(new RouteNode(wc.getTile(),wc.getWireIndex()));
            }
        }

        while (!queue.isEmpty()) {
            RouteNode currNode = queue.poll();
            Integer i = reachabilityMap.get(currNode.getTile());
            if (i == null) {
                i = 1;
                reachabilityMap.put(currNode.getTile(), i);
            }
            else {
                reachabilityMap.put(currNode.getTile(), i+1);
            }
            if (currNode.getLevel() < hops-1) {
                List<Wire> connections = currNode.getConnections();
                for (Wire wc : connections) {
                    queue.add(new RouteNode(wc.getTile(),wc.getWireIndex()));
                }
            }
        }
        return reachabilityMap;
    }

    private void drawReachability(HashMap<Tile, Integer> map) {
        menuReachabilityClear();
        for (Tile t : map.keySet()) {
            int color = map.get(t)*16 > 255 ? 255 : map.get(t)*16;
            NumberedHighlightedTile tile = new NumberedHighlightedTile(t, this, map.get(t));
            tile.setBrush(new QBrush(new QColor(0, color, 0)));
            currentTiles.add(tile);
        }
    }

    @SuppressWarnings("unused")
    private void menuReachability1() {
        drawReachability(findReachability(reachabilityTile, 1));
    }

    @SuppressWarnings("unused")
    private void menuReachability2() {
        drawReachability(findReachability(reachabilityTile, 2));
    }

    @SuppressWarnings("unused")
    private void menuReachability3() {
        drawReachability(findReachability(reachabilityTile, 3));
    }

    @SuppressWarnings("unused")
    private void menuReachability4() {
        drawReachability(findReachability(reachabilityTile, 4));
    }

    @SuppressWarnings("unused")
    private void menuReachability5() {
        drawReachability(findReachability(reachabilityTile, 5));
    }

    private void menuReachabilityClear() {
        clearHighlightedTiles();
    }


    public void addHighlightedTile(NumberedHighlightedTile tile) {
        currentTiles.add(tile);
    }

    public void clearHighlightedTiles() {
        for (NumberedHighlightedTile rect : currentTiles) {
            rect.remove();
        }
        currentTiles.clear();
    }


    @Override
    public void mouseDoubleClickEvent(QGraphicsSceneMouseEvent event) {
        Tile t = getTile(event);
        this.updateTile.emit(t);
        super.mouseDoubleClickEvent(event);
    }

    @Override
    public void mouseReleaseEvent(QGraphicsSceneMouseEvent event) {
        if (event.button().equals(MouseButton.RightButton)) {
            if (browser.view.hasPanned) {
                browser.view.hasPanned = false;

            }
            else {
                reachabilityTile = getTile(event);
                QMenu menu = new QMenu();
                QAction action1 = new QAction("Draw Reachability (1 Hop)", this);
                QAction action2 = new QAction("Draw Reachability (2 Hops)", this);
                QAction action3 = new QAction("Draw Reachability (3 Hops)", this);
                QAction action4 = new QAction("Draw Reachability (4 Hops)", this);
                QAction action5 = new QAction("Draw Reachability (5 Hops)", this);
                QAction actionClear = new QAction("Clear Highlighted Tiles", this);
                action1.triggered.connect(this, "menuReachability1()");
                action2.triggered.connect(this, "menuReachability2()");
                action3.triggered.connect(this, "menuReachability3()");
                action4.triggered.connect(this, "menuReachability4()");
                action5.triggered.connect(this, "menuReachability5()");
                actionClear.triggered.connect(this, "menuReachabilityClear()");
                menu.addAction(action1);
                menu.addAction(action2);
                menu.addAction(action3);
                menu.addAction(action4);
                menu.addAction(action5);
                menu.addAction(actionClear);
                menu.exec(event.screenPos());
            }
        }


        super.mouseReleaseEvent(event);
    }

    public List<TileGroup> tileGroups;

    public void highlightTileGroups() {
        for (TileGroup tg : tileGroups) {
            highlightTileGroup(tg);
        }
    }

    public void highlightTileGroup(TileGroup tg) {
        QBrush brush = new QBrush(Qt.GlobalColor.red);
        Map<Tile, Edge> tileMap = tg.getRegionTiles();
        for (Entry<Tile, Edge> e : tileMap.entrySet()) {
            if (e.getValue() == Edge.INTERNAL)
                continue;
            NumberedHighlightedTile highTile = new NumberedHighlightedTile(e.getKey(), this, null);
            highTile.setBrush(brush);
            addHighlightedTile(highTile);
        }
    }

    @Override
    public boolean event(QEvent event) {
        boolean result = true;

        switch(event.type().value()) {
            case CLEAR_HIGHLIGHTED_TILES:
                clearHighlightedTiles();
                break;
            case HIGHLIGHT_TILE_GROUPS:
                highlightTileGroups();
                break;
            default:
                result = super.event(event);
            }
        return result;
    }
}
