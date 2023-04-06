package com.flowci.parser.v2;

import com.flowci.exception.YmlException;
import com.flowci.parser.v2.yml.FlowYml;
import com.flowci.util.YamlHelper;
import org.yaml.snakeyaml.error.YAMLException;

public class YmlParser {

    public static FlowYml load(String... ymls) {
        try {
            var root = new FlowYml();

            for (var yml : ymls) {
                var ymlObj = YamlHelper.create(FlowYml.class);
                FlowYml tmp = ymlObj.load(yml);
                root.merge(tmp);
            }

            return root;
        } catch (YAMLException e) {
            throw new YmlException(e.getMessage());
        }
    }
}
