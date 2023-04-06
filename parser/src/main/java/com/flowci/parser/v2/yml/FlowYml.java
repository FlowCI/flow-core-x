package com.flowci.parser.v2.yml;

import com.flowci.exception.YmlException;
import com.flowci.util.ObjectsHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.DuplicateFormatFlagsException;

@Getter
@Setter
public class FlowYml extends NodeYml {

    private String name;

    private Integer version;

    @SneakyThrows
    public void merge(FlowYml other) {
        try {
            ObjectsHelper.merge(other, this);
        } catch (DuplicateFormatFlagsException e) {
            throw new YmlException(e.getMessage());
        }
    }
}
