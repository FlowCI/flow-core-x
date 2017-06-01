package com.flow.platform.util.mos;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by gy@fir.im on 01/06/2017.
 * Copyright fir.im
 */
public class ImageTemplate implements Serializable {

    private Long size;

    private String templateId;

    private String templateName;

    private String checksum;

    @SerializedName("is_public")
    private Boolean isPublic;

    private String status;

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    public void setPublic(Boolean aPublic) {
        isPublic = aPublic;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageTemplate that = (ImageTemplate) o;

        return templateId.equals(that.templateId);
    }

    @Override
    public int hashCode() {
        return templateId.hashCode();
    }

    @Override
    public String toString() {
        return "MosImageTemplate{" +
                "size=" + size +
                ", templateId='" + templateId + '\'' +
                ", templateName='" + templateName + '\'' +
                ", checksum='" + checksum + '\'' +
                ", isPublic=" + isPublic +
                ", status='" + status + '\'' +
                '}';
    }
}
