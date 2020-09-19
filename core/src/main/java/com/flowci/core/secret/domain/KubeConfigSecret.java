package com.flowci.core.secret.domain;

import com.flowci.domain.SecretField;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "secret")
public class KubeConfigSecret extends Secret {

    private SecretField content;

    public KubeConfigSecret() {
        setCategory(Category.KUBE_CONFIG);
    }
}
