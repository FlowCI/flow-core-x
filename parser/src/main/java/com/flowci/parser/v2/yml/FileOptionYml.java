package com.flowci.parser.v2.yml;

import com.flowci.domain.tree.FileOption;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class FileOptionYml implements Convertable<FileOption> {

    private String name;

    private List<String> paths;

    @Override
    public FileOption convert() {
        return FileOption.builder()
                .name(name)
                .paths(paths)
                .build();
    }
}
