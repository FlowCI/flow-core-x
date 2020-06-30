package com.flowci.core.config.domain;

import com.flowci.domain.SecretField;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yang
 */
@Getter
@Setter
public class AndroidSignConfig extends Config {

    private String keyStoreFile; // url link

    private SecretField keyStorePassword;

    private String keyAlias;

    private SecretField keyPassword;

    public AndroidSignConfig() {
        setCategory(Category.ANDROID_SIGN);
    }
}
