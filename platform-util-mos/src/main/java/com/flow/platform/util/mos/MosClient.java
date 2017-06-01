package com.flow.platform.util.mos;

import com.google.gson.Gson;
import com.meituan.mos.sdk.v1.Client;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by gy@fir.im on 01/06/2017.
 * Copyright fir.im
 */
public class MosClient {

    private final static Gson GSON = new Gson();

    private final static String DEFAULT_API_URL = "https://mosapi.meituan.com/mcs/v1";
    private final static String DEFAULT_REGION = "Beijing";

    private final static String DEFAULT_NET_ID = "3d6bce0f-bfa0-4117-a537-075b24a78f28";
    private final static String DEFAULT_KEY_NAME = "53f9459b-2bfc-48c3-9bf8-fb59b8cc077f";
    private final static String DEFAULT_ZONE_ID = "df2e198b-a8cd-45a5-8a9e-d6314e809031"; // 北京4区, runze
    private final static String DEFAULT_INSTANCE_TYPE = "781f1634-da01-4762-9702-af82f3fa3911";
    private final static String DEFAULT_DURATION = "1H";
    private final static String DEFAULT_GROUP_ID = "0907776e-5784-4d25-8ce1-81689084fcbb";

    private final static int DEFAULT_TIMEOUT = 10; // request timeout in seconds

    private final Client client;

    private final List<ImageTemplate> imageTemplates = new LinkedList<>();

    private final List<Zone> zones = new LinkedList<>();

    public MosClient(String key, String secret) throws Throwable {
        client = new Client(key, secret, DEFAULT_API_URL, DEFAULT_REGION, null, DEFAULT_TIMEOUT, false);

        // init image templates
        initImageTemplate();

        // init zones
        initAvailableZones();
    }

    public List<ImageTemplate> getImageTemplates() {
        return imageTemplates;
    }

    public ImageTemplate getImageTemplate(String imageName) {
        for (ImageTemplate template : imageTemplates) {
            if (template.getTemplateName().equals(imageName)) {
                return template;
            }
        }
        return null;
    }

    public List<Zone> getZones() {
        return zones;
    }

    public Instance createInstance(String imageName, String instanceName) throws Exception {
        ImageTemplate template = getImageTemplate(imageName);
        JSONObject result = client.CreateInstance(
                template.getTemplateId(),
                DEFAULT_INSTANCE_TYPE,
                DEFAULT_KEY_NAME,
                0,
                0,
                null,
                DEFAULT_DURATION,
                instanceName,
                DEFAULT_ZONE_ID,
                DEFAULT_GROUP_ID
        );

        Instance instance = GSON.fromJson(result.toString(), Instance.class);
        if (instance.getInstanceId() == null) {
            new MosError.CreateInstanceException("Missing instance id");
        }

        // bind net

        return instance;
    }

    private void initAvailableZones() throws Exception {
        JSONObject jsonObject = client.DescribeAvailabilityZones(100, 0);
        JSONArray zoneList = (JSONArray) jsonObject.get("AvailabilityZone");
        for (int i = 0; i < zoneList.length(); i++) {
            JSONObject element = zoneList.getJSONObject(i);
            zones.add(GSON.fromJson(element.toString(), Zone.class));
        }
    }

    private void initImageTemplate() throws Exception {
        JSONObject jsonObject = client.DescribeTemplates();
        JSONArray imageList = (JSONArray) jsonObject.get("Template");
        for (int i = 0; i < imageList.length(); i++) {
            JSONObject element = imageList.getJSONObject(i);
            imageTemplates.add(GSON.fromJson(element.toString(), ImageTemplate.class));
        }
    }
}
