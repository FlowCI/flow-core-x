/*
 * Copyright 2018 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.common.helper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.CollectionNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * @author yang
 */
public class YamlOrderedSkipEmptyRepresenter extends Representer {

    private final PropertySorter sorter = new PropertySorter();

    private class PropertySorter implements Comparator<Property> {

        @Override
        public int compare(Property o1, Property o2) {
            Integer index1 = order.get(o1.getName());
            Integer index2 = order.get(o2.getName());

            if (Objects.isNull(index1) || Objects.isNull(index2)) {
                return 0;
            }

            return index1.compareTo(index2);
        }
    }

    private final Map<String, Integer> order;

    public YamlOrderedSkipEmptyRepresenter(Map<String, Integer> fieldsOrder) {
        super(new DumperOptions());
        this.order = fieldsOrder;
    }

    @Override
    protected MappingNode representJavaBean(Set<Property> properties, Object javaBean) {
        List<NodeTuple> value = new ArrayList<>(properties.size());
        Tag tag;
        Tag customTag = classTags.get(javaBean.getClass());
        tag = customTag != null ? customTag : new Tag(javaBean.getClass());
        MappingNode node = new MappingNode(tag, value, FlowStyle.BLOCK);
        representedObjects.put(javaBean, node);

        List<Property> orderProperties = new ArrayList<>(properties);
        orderProperties.sort(sorter);

        for (Property property : orderProperties) {
            Object memberValue = property.get(javaBean);
            Tag customPropertyTag = memberValue == null ? null
                : classTags.get(memberValue.getClass());
            NodeTuple tuple = representJavaBeanProperty(javaBean, property, memberValue,
                customPropertyTag);
            if (tuple == null) {
                continue;
            }

            value.add(tuple);
        }

        return node;
    }

    @Override
    protected NodeTuple representJavaBeanProperty(Object javaBean,
                                                  Property property,
                                                  Object propertyValue,
                                                  Tag customTag) {

        NodeTuple tuple = super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
        org.yaml.snakeyaml.nodes.Node valueNode = tuple.getValueNode();

        // skip 'null' values
        if (Tag.NULL.equals(valueNode.getTag())) {
            return null;
        }

        if (valueNode instanceof CollectionNode) {

            // skip empty lists
            if (Tag.SEQ.equals(valueNode.getTag())) {
                SequenceNode seq = (SequenceNode) valueNode;
                if (seq.getValue().isEmpty()) {
                    return null;
                }
            }

            // skip empty maps
            if (Tag.MAP.equals(valueNode.getTag())) {
                MappingNode seq = (MappingNode) valueNode;
                if (seq.getValue().isEmpty()) {
                    return null;
                }
            }
        }

        return tuple;
    }
}
