package com.flowci.parser.v2.yml;

import com.flowci.domain.tree.DockerOption;
import com.flowci.exception.YmlException;
import com.flowci.util.ObjectsHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.DuplicateFormatFlagsException;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class FlowYml {

    private String name;

    private Integer version;

    private Map<String, String> vars;

    private String condition;

    private DockerOption docker;

    private List<String> agents;

    @SneakyThrows
    public void merge(FlowYml other) {
        try {
            ObjectsHelper.merge(other, this);
        } catch (DuplicateFormatFlagsException e) {
            throw new YmlException(e.getMessage());
        }
    }
}
