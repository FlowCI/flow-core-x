package com.flowci.core.config.domain;

import com.flowci.domain.SecretField;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yang
 */
@Getter
@Setter
@Document(collection = "configuration")
public class AndroidSignConfig extends Config {

    private String keyStoreFileUrl;

    private String keyStoreFileName;

    private SecretField keyStorePassword;

    private String keyAlias;

    private SecretField keyPassword;

    public AndroidSignConfig() {
        setCategory(Category.ANDROID_SIGN);
    }
}
