package com.flow.platform.util.mos;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.meituan.mos.sdk.v1.Client;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by gy@fir.im on 01/06/2017.
 * Copyright fir.im
 */
public class MosClient {

    private static class MosSizeInfo {

        @SerializedName("Total")
        private int total;

        @SerializedName("Limit")
        private int limit;

        @SerializedName("Offset")
        private int offset;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }
    }

    private final static Gson GSON = new GsonBuilder().create();

    private final static String DEFAULT_API_URL = MosConfig.DEFAULT_API_URL;
    private final static String DEFAULT_REGION = MosConfig.DEFAULT_REGION;

    private final static String DEFAULT_NET_ID = MosConfig.DEFAULT_NET_ID;
    private final static String DEFAULT_KEY_NAME = MosConfig.DEFAULT_KEY_NAME;
    private final static String DEFAULT_ZONE_ID = MosConfig.DEFAULT_ZONE_ID;
    private final static String DEFAULT_INSTANCE_TYPE = MosConfig.DEFAULT_INSTANCE_TYPE;
    private final static String DEFAULT_DURATION = MosConfig.DEFAULT_DURATION;
    private final static String DEFAULT_GROUP_ID = MosConfig.DEFAULT_GROUP_ID;

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

    /**
     * Instance list
     *
     * @return instance list
     */
    public List<Instance> listInstance() {
        JSONObject response = null;
        List<Instance> list = null;
        Object rawInstances = null;

        try {
            response = client.DescribeInstances(null, null, 100, 0, null);
            JSONObject jsonInstanceSet = response
                    .getJSONObject("DescribeInstancesResponse")
                    .getJSONObject("InstanceSet");

            try {
                 rawInstances = jsonInstanceSet.get("Instance");
            } catch (JSONException e) {
                // cannot find instance data since data is out of range
                MosSizeInfo sizeInfo = GSON.fromJson(jsonInstanceSet.toString(), MosSizeInfo.class);
                return new ArrayList<>(0);
            }

            // only one instance
            if (rawInstances instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) rawInstances;
                Instance instance = GSON.fromJson(jsonObject.toString(), Instance.class);

                list = new ArrayList<>(1);
                list.add(instance);
                return list;
            }

            if (rawInstances instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) rawInstances;
                list = new ArrayList<>(jsonArray.length());

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonItem = jsonArray.getJSONObject(i);
                    Instance instance = GSON.fromJson(jsonItem.toString(), Instance.class);
                    list.add(instance);
                }

                return list;
            }

            throw new IllegalStateException("Data cannot be convert to Instance object");

        } catch (JSONException e) {
            MosException mosException = new MosException("DescribeInstances: Wrong response data", e);
            mosException.setError(response);
            throw mosException;
        } catch (Throwable e) {
            throw new MosException("DescribeInstances: Exception from request", e);
        }
    }

    public Instance createInstance(String imageName, String instanceName) {
        ImageTemplate template = getImageTemplate(imageName);

        Instance instance = null;
        JSONObject result = null;
        try {
            result = client.CreateInstance(
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

            JSONObject jsonObject = result.getJSONObject("CreateInstanceResponse").getJSONObject("Instance");
            instance = GSON.fromJson(jsonObject.toString(), Instance.class);
            if (instance == null || instance.getInstanceId() == null) {
                throw new IllegalStateException("Missing instance id, maybe duplicate instance name");
            }
        } catch (JSONException e) {
            MosException mosException = new MosException("CreateInstance: wrong response data", e);
            mosException.setError(result);
            throw mosException;
        } catch (Throwable e) {
            throw new MosException("CreateInstance: Fail to create instance", e);
        }

        // bind nat gateway if instance created
        try {
            if (!bindNatGateway(instance.getInstanceId())) {
                String msg = String.format("Fail to bind nat gateway for instance: %s, return false",
                        instance.getInstanceId());

                throw new MosException(msg, null, instance);
            }
        } catch (Throwable e) {
            throw new MosException(e.getMessage(), e, instance);
        }

        return instance;
    }

    public boolean bindNatGateway(String instanceId) {
        JSONObject result = null;
        try {
            this.instanceStatusSync(instanceId, Instance.STATUS_RUNNING, 40 * 1000); // wait mos instance running
            result = client.AssociateNatGateway(DEFAULT_NET_ID, instanceId, DEFAULT_ZONE_ID);
            return result.getJSONObject("AssociateNatGatewayResponse").getBoolean("return");
        } catch (JSONException e) {
            MosException mosException = new MosException("BindNatGateWay: Wrong response data", e);
            mosException.setError(result);
            throw mosException;
        } catch (Throwable e) {
            throw new MosException("BindNatGateWay: Fail to bind nat gateway for instance: " + instanceId, e);
        }
    }

    public List<NatGateway> getNatGateWay() {
        JSONObject jsonObject = null;
        try {
            jsonObject = client.DescribeNatGateway(100, 0);

            Object rawData = jsonObject
                    .getJSONObject("DescribeNatGatewayResponse")
                    .getJSONObject("NatGatewaySet")
                    .get("NatGateway");

            List<NatGateway> gateways = new ArrayList<>(10);

            if (rawData instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) rawData;
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject element = jsonArray.getJSONObject(i);
                    gateways.add(GSON.fromJson(element.toString(), NatGateway.class));
                }
                return gateways;
            }

            if (rawData instanceof JSONObject) {
                NatGateway obj = GSON.fromJson(rawData.toString(), NatGateway.class);
                gateways.add(obj);
                return gateways;
            }

            throw new IllegalStateException("Data cannot be convert to NatGateway object");

        } catch (JSONException e) {
            MosException mosException = new MosException("NatGateway: Wrong response data", e);
            mosException.setError(jsonObject);
            throw mosException;
        } catch (Throwable e) {
            throw new MosException("NatGateway: Exception from request", e);
        }
    }

    public void deleteInstance(String instanceId) {
        try {
            client.TerminateInstance(instanceId);
        } catch (Throwable e) {
            throw new MosException("DeleteInstance: Exception from request", e);
        }
    }

    public void stopInstance(String instanceId, boolean force) {
        try {
            client.StopInstance(instanceId, force);
        } catch (Throwable e) {
            throw new MosException("StopInstance: Exception from request", e);
        }
    }

    public String instanceStatus(String instanceId) {
        try {
            JSONObject response = client.DescribeInstanceStatus(instanceId);
            return response.getJSONObject("DescribeInstanceStatusResponse")
                    .getJSONObject("InstanceStatus")
                    .getString("status");
        } catch (JSONException e) {
            throw new MosException("InstanceStatus: Wrong response data, maybe instance doesn't exist.", e);
        } catch (Throwable e) {
            throw new MosException("InstanceStatus: Exception from request", e);
        }
    }

    /**
     * Periodically retrive instance status, exit until got expect stauts or timeout
     *
     * @param instanceId
     * @param expectStatus
     * @param timeout      in millseconds
     * @return boolean is status loaded or not
     */
    public boolean instanceStatusSync(final String instanceId, final String expectStatus, final long timeout) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean canExit = new AtomicBoolean(false);
        final AtomicBoolean canLoadInstance = new AtomicBoolean(true);

        try {
            Thread loadInstanceThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!canExit.get()) {
                            String loadedStatus = instanceStatus(instanceId);
                            if (Objects.equals(expectStatus, loadedStatus)) {
                                latch.countDown();
                                break;
                            }
                            Thread.sleep(1000);
                        }
                    } catch (Throwable e) {
                        canLoadInstance.set(false);
                        latch.countDown();
                    }
                }
            });

            loadInstanceThread.setDaemon(true);
            loadInstanceThread.start();

            if (latch.await(timeout, TimeUnit.MILLISECONDS) && canLoadInstance.get()) {
                return true;
            }
            return false;
        } catch (InterruptedException e) {
            return false;
        } finally {
            canExit.set(true);
        }
    }


    private void initAvailableZones() {
        JSONObject raw = null;
        try {
            raw = client.DescribeAvailabilityZones(100, 0);

            JSONArray zoneList = raw
                    .getJSONObject("DescribeAvailabilityZonesResponse")
                    .getJSONObject("AvailabilityZoneSet")
                    .getJSONArray("AvailabilityZone");

            for (int i = 0; i < zoneList.length(); i++) {
                JSONObject element = zoneList.getJSONObject(i);
                zones.add(GSON.fromJson(element.toString(), Zone.class));
            }
        } catch (JSONException e) {
            MosException mosException = new MosException("AvailableZones: Wrong response data", e);
            mosException.setError(raw);
            throw mosException;
        } catch (Exception e) {
            throw new MosException("AvailableZones: Exception from request", e);
        }
    }

    private void initImageTemplate() {
        JSONObject raw = null;
        try {
            raw = client.DescribeTemplates();

            JSONArray imageList = raw
                    .getJSONObject("DescribeTemplatesResponse")
                    .getJSONObject("TemplateSet")
                    .getJSONArray("Template");

            for (int i = 0; i < imageList.length(); i++) {
                JSONObject element = imageList.getJSONObject(i);
                imageTemplates.add(GSON.fromJson(element.toString(), ImageTemplate.class));
            }
        } catch (JSONException jsonException) {
            MosException mosException = new MosException("ImageTemplate: Wrong response data", jsonException);
            mosException.setError(raw);
            throw mosException;
        } catch (Exception e) {
            throw new MosException("ImageTemplate: Exception from request", e);
        }
    }
}
