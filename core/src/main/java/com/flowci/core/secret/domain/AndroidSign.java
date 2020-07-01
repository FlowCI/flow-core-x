package com.flowci.core.secret.domain;

import com.flowci.domain.SecretField;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yang
 */
@Getter
@Setter
@Document(collection = "secret")
public class AndroidSign extends Secret {

    private String keyStoreFileName;

    private SecretField keyStorePassword;

    private String keyAlias;

    private SecretField keyPassword;

    public AndroidSign() {
        setCategory(Category.ANDROID_SIGN);
    }
}
