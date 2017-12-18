/*
 * Copyright 2017 flow.ci
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
package com.flow.platform.api.util;

import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.exception.YmlException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.CollectionNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * @author yh@firim
 */
public class NodeUtil {

    private final static DumperOptions DUMPER_OPTIONS = new DumperOptions();

    private final static LineBreak LINE_BREAK = LineBreak.getPlatformLineBreak();

    private final static Constructor ROOT_YML_CONSTRUCTOR = new Constructor(RootYmlWrapper.class);

    private final static Representer ORDERED_SKIP_EMPTY_REPRESENTER = new OrderedAndSkipEmptyRepresenter();

    static {
        DUMPER_OPTIONS.setIndent(4);
        DUMPER_OPTIONS.setIndicatorIndent(2);
        DUMPER_OPTIONS.setExplicitStart(true);
        DUMPER_OPTIONS.setDefaultFlowStyle(FlowStyle.BLOCK);
        DUMPER_OPTIONS.setLineBreak(LINE_BREAK);

        TypeDescription nodeTypeDesc = new TypeDescription(NodeWrapper.class);
        ROOT_YML_CONSTRUCTOR.addTypeDescription(nodeTypeDesc);
    }

    /**
     * Get node workspace path
     */
    public static Path workspacePath(Path base, Node node) {
        return Paths.get(base.toString(), node.getName());
    }

    public static String parseToYml(Node root) {
        RootYmlWrapper ymlWrapper = new RootYmlWrapper(new NodeWrapper(root));

        // clean property which not used in root
        ymlWrapper.flow.get(0).name = null;
        ymlWrapper.flow.get(0).allowFailure = null;

        Yaml yaml = new Yaml(ROOT_YML_CONSTRUCTOR, ORDERED_SKIP_EMPTY_REPRESENTER, DUMPER_OPTIONS);
        String dump = yaml.dump(ymlWrapper);
        dump = dump.substring(dump.indexOf(LINE_BREAK.getString()) + 1);
        return dump;
    }

    /**
     * Build node tree structure from yml string
     *
     * @param yml raw yml string
     * @return root node of yml
     * @throws YmlException if yml format is illegal
     */
    public static Node buildFromYml(String yml, String rootName) {
        try {
            Yaml yaml = new Yaml(ROOT_YML_CONSTRUCTOR);
            RootYmlWrapper node = yaml.load(yml);

            // verify flow node
            if (Objects.isNull(node.flow)) {
                throw new YmlException("The 'flow' content must be defined");
            }

            // current version only support single flow
            if (node.flow.size() > 1) {
                throw new YmlException("Unsupported multiple flows definition");
            }

            // steps must be provided
            List<NodeWrapper> steps = node.flow.get(0).steps;
            if (Objects.isNull(steps) || steps.isEmpty()) {
                throw new YmlException("The 'step' must be defined");
            }

            node.flow.get(0).name = rootName;
            Node root = node.flow.get(0).toNode();

            buildNodeRelation(root);
            return root;
        } catch (YAMLException e) {
            throw new YmlException(e.getMessage());
        }
    }

    /**
     * find all node
     */
    public static void recurse(final Node root, final Consumer<Node> onNode) {
        for (Object child : root.getChildren()) {
            recurse((Node) child, onNode);
        }
        onNode.accept(root);
    }

    /**
     * get all nodes includes flow and steps
     *
     * @param node the parent node
     * @return {@code List<Node>} include parent node
     */
    public static List<Node> flat(final Node node) {
        final List<Node> flatted = new LinkedList<>();
        recurse(node, flatted::add);
        return flatted;
    }

    /**
     * find flow node
     */
    public static Node findRootNode(Node node) {
        if (node.getParent() == null) {
            return node;
        }

        return findRootNode(node.getParent());
    }

    /**
     * get prev node from flow
     */
    public static Node prev(Node node, List<Node> ordered) {
        Integer index = ordered.indexOf(node);

        if (index == -1) {
            return null;
        }

        if (index >= 1 && index <= ordered.size() - 1) {
            return ordered.get(index - 1);
        }

        return null;
    }

    /**
     * Get next node from current node
     *
     * @param node current node
     * @param ordered ordered and flatted node list
     * @return next node of current
     */
    public static Node next(Node node, List<Node> ordered) {
        Integer index = ordered.indexOf(node);

        if (index == -1) {
            return null;
        }

        // find node
        try {
            return ordered.get(index + 1);
        } catch (Throwable ignore) {
            return null;
        }
    }

    /**
     * Build node path and parent, next, prev relation
     */
    public static void buildNodeRelation(Node node) {
        Node parent = node.getParent();
        node.setPath(Objects.isNull(parent) ? node.getName() : PathUtil.build(parent.getPath(), node.getName()));

        List<Node> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Node childNode = children.get(i);
            childNode.setParent(node);
            if (i > 0) {
                childNode.setPrev(children.get(i - 1));
                children.get(i - 1).setNext(childNode);
            }

            buildNodeRelation(childNode);
        }
    }

    /**
     * Represent YML root flow
     */
    private static class RootYmlWrapper {

        public List<NodeWrapper> flow;

        public RootYmlWrapper() {
        }

        public RootYmlWrapper(NodeWrapper root) {
            this.flow = Lists.newArrayList(root);
        }
    }

    /**
     * Represent Node instance in YML
     */
    private static class NodeWrapper {

        public String name;

        public Map<String, String> envs;

        public Boolean allowFailure;

        public Boolean isFinal;

        public String condition;

        public List<NodeWrapper> steps;

        public String script;

        public String plugin;

        public NodeWrapper() {
        }

        /**
         * Used for parse node to yml
         */
        public NodeWrapper(Node node) {
            name = node.getName();
            if (Strings.isNullOrEmpty(name)) {
                throw new YmlException("The node name is required");
            }

            envs = node.getEnvs();
            allowFailure = !node.getAllowFailure() ? null : node.getAllowFailure();
            isFinal = !node.getIsFinal() ? null : node.getIsFinal();
            condition = node.getConditionScript();
            script = node.getScript();
            plugin = node.getPlugin();
            steps = new LinkedList<>();

            verify();

            List<Node> children = node.getChildren();
            Set<String> existNameSet = new HashSet<>(children.size());

            for (Node child : children) {
                if (!existNameSet.add(child.getName())) {
                    throw new YmlException("The step name '" + child.getName() + "'is not unique");
                }

                steps.add(new NodeWrapper(child));
            }
        }

        /**
         * Used for build from yml to node
         */
        public Node toNode() {
            Node node = new Node();
            node.setName(name);
            node.setConditionScript(condition);
            node.setScript(script);
            node.setPlugin(plugin);
            node.setAllowFailure(Objects.isNull(allowFailure) ? false : allowFailure);
            node.setIsFinal(Objects.isNull(isFinal) ? false : isFinal);

            if (!Objects.isNull(envs)) {
                node.setEnvs(envs);
            }

            verify();

            if (Objects.isNull(steps)) {
                return node;
            }

            Set<String> existNameSet = new HashSet<>(steps.size());
            for (NodeWrapper wrapper : steps) {
                if (!existNameSet.add(wrapper.name)) {
                    throw new YmlException("The step name '" + wrapper.name + "'is not unique");
                }

                node.getChildren().add(wrapper.toNode());
            }

            return node;
        }

        private void verify() {
            if (!Strings.isNullOrEmpty(script) && !Strings.isNullOrEmpty(plugin)) {
                throw new YmlException("The script and plugin cannot defined in both");
            }
        }
    }

    /**
     * Snakeyaml representer which support output property order and skip empty or null value
     */
    private static class OrderedAndSkipEmptyRepresenter extends Representer {

        private final Map<String, Integer> order = ImmutableMap.<String, Integer>builder()
            .put("name", 1)
            .put("envs", 2)
            .put("allowFailure", 3)
            .put("isFinal", 4)
            .put("condition", 5)
            .put("plugin", 6)
            .put("script", 7)
            .put("steps", 8)
            .build();

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

        @Override
        protected MappingNode representJavaBean(Set<Property> properties, Object javaBean) {
            List<NodeTuple> value = new ArrayList<>(properties.size());
            Tag tag;
            Tag customTag = classTags.get(javaBean.getClass());
            tag = customTag != null ? customTag : new Tag(javaBean.getClass());
            MappingNode node = new MappingNode(tag, value, null);
            representedObjects.put(javaBean, node);
            boolean bestStyle = true;

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
                if (((ScalarNode) tuple.getKeyNode()).getStyle() != null) {
                    bestStyle = false;
                }
                org.yaml.snakeyaml.nodes.Node nodeValue = tuple.getValueNode();
                if (!(nodeValue instanceof ScalarNode && ((ScalarNode) nodeValue).getStyle() == null)) {
                    bestStyle = false;
                }
                value.add(tuple);
            }
            if (defaultFlowStyle != FlowStyle.AUTO) {
                node.setFlowStyle(defaultFlowStyle.getStyleBoolean());
            } else {
                node.setFlowStyle(bestStyle);
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
}
