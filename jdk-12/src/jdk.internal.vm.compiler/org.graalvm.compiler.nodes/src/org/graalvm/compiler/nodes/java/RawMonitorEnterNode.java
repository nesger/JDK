/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */


package org.graalvm.compiler.nodes.java;

import static org.graalvm.compiler.nodeinfo.NodeCycles.CYCLES_64;
import static org.graalvm.compiler.nodeinfo.NodeSize.SIZE_64;

import org.graalvm.compiler.core.common.type.ObjectStamp;
import org.graalvm.compiler.graph.IterableNodeType;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.NodeView;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.extended.MonitorEnter;
import org.graalvm.compiler.nodes.memory.MemoryCheckpoint;
import org.graalvm.compiler.nodes.spi.Lowerable;
import org.graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.compiler.nodes.spi.Virtualizable;
import org.graalvm.compiler.nodes.spi.VirtualizerTool;
import org.graalvm.compiler.nodes.virtual.VirtualObjectNode;
import jdk.internal.vm.compiler.word.LocationIdentity;

/**
 * The {@code RawMonitorEnterNode} represents the acquisition of a monitor. The object needs to
 * already be non-null and the hub is an additional parameter to the node.
 */
// @formatter:off
@NodeInfo(cycles = CYCLES_64,
          cyclesRationale = "Rough estimation of the enter operation",
          size = SIZE_64)
// @formatter:on
public final class RawMonitorEnterNode extends AccessMonitorNode implements Virtualizable, Lowerable, IterableNodeType, MonitorEnter, MemoryCheckpoint.Single {

    public static final NodeClass<RawMonitorEnterNode> TYPE = NodeClass.create(RawMonitorEnterNode.class);

    @Input ValueNode hub;

    public RawMonitorEnterNode(ValueNode object, ValueNode hub, MonitorIdNode monitorId) {
        super(TYPE, object, monitorId);
        assert ((ObjectStamp) object.stamp(NodeView.DEFAULT)).nonNull();
        this.hub = hub;
    }

    @Override
    public LocationIdentity getLocationIdentity() {
        return LocationIdentity.any();
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public void virtualize(VirtualizerTool tool) {
        ValueNode alias = tool.getAlias(object());
        if (alias instanceof VirtualObjectNode) {
            VirtualObjectNode virtual = (VirtualObjectNode) alias;
            if (virtual.hasIdentity()) {
                tool.addLock(virtual, getMonitorId());
                tool.delete();
            }
        }
    }

    public ValueNode getHub() {
        return hub;
    }
}
