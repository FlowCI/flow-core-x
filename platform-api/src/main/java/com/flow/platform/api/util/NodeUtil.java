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

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.exception.YmlException;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.yml.parser.YmlParser;
import com.flow.platform.yml.parser.exception.YmlParserException;
import com.flow.platform.yml.parser.exception.YmlValidatorException;
import com.google.common.io.Files;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.yaml.snakeyaml.Yaml;

/**
 * @author yh@firim
 */
public class NodeUtil {

    /**
     * Get node workspace path
     */
    public static Path workspacePath(Path base, Node node) {
        return Paths.get(base.toString(), node.getName());
    }

    /**
     * Build node tree structure from yml file
     *
     * @param path file path
     * @return node tree
     */
    public static Node buildFromYml(File path) {
        try {
            String ymlString = Files.toString(path, AppConfig.DEFAULT_CHARSET);
            return buildFromYml(ymlString);
        } catch (YmlException e) {
            throw e;
        } catch (Throwable ignore) {
            return null;
        }
    }

    /**
     * Build node tree structure from yml string
     *
     * @param yml raw yml string
     * @return root node of yml
     * @throws YmlException if yml format is illegal
     */
    public static Node buildFromYml(String yml) {

        Flow[] flows;
        try {
            flows = YmlParser.fromYml(yml, Flow[].class);
        }catch (YmlParserException e){
            throw new YmlException("Yml parser error", e);
        }catch (YmlValidatorException e){
            throw new YmlException("Yml validator error", e);
        }

        // current version only support single flow
        if (flows.length > 1) {
            throw new YmlException("Unsupported multiple flows definition");
        }

        buildNodeRelation(flows[0]);
        return flows[0];
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
     * @return List<Node> include parent node
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
        Integer index = -1;
        Node target = null;

        for (int i = 0; i < ordered.size(); i++) {
            Node item = ordered.get(i);
            if (item.equals(node)) {
                index = i;
            }
        }

        if (index >= 1 && index != -1 && index < ordered.size() - 1) {
            target = ordered.get(index - 1);
        }

        return target;
    }

    /**
     * get next node compare root
     *
     * @param node current node
     * @param ordered ordered and flatted node tree
     * @return next node of current
     */
    public static Node next(Node node, List<Node> ordered) {
        Integer index = -1;
        Node target = null;

        //find node
        for (int i = 0; i < ordered.size(); i++) {
            Node item = ordered.get(i);
            if (item.equals(node)) {
                index = i;
                break;
            }
        }

        // find node
        if (index != -1) {
            try {
                target = ordered.get(index + 1);
            } catch (Throwable ignore) {
                //not found ignore
                target = null;
            }
        }

        return target;
    }

    /**
     * Build node path, parent, next, prev relation
     */
    public static void buildNodeRelation(Node<? extends Node> root) {
        setNodePath(root);

        List<? extends Node> children = root.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Node childNode = children.get(i);
            childNode.setParent(root);
            if (i > 0) {
                childNode.setPrev(children.get(i - 1));
                children.get(i - 1).setNext(childNode);
            }

            buildNodeRelation(childNode);
        }
    }

    private static void setNodePath(Node node) {
        if (node.getParent() == null) {
            node.setPath(PathUtil.build(node.getName()));
            return;
        }
        node.setPath(PathUtil.build(node.getParent().getPath(), node.getName()));
    }
}
