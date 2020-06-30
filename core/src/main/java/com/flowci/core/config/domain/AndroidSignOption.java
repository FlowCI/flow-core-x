package com.flowci.core.config.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public final class AndroidSignOption {

    private MultipartFile keyStore;

    @NotEmpty
    private String keyStorePw;

    @NotEmpty
    private String keyAlias;

    @NotEmpty
    private String keyPw;
}
