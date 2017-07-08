package com.flow.platform.util.mos;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.meituan.mos.sdk.v1.Client;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private final static String DEFAULT_SSH_KEY_NAME = MosConfig.DEFAULT_SSH_KEY_NAME;
    private final static String DEFAULT_ZONE_ID = MosConfig.DEFAULT_ZONE_ID;
    private final static String DEFAULT_INSTANCE_TYPE = MosConfig.DEFAULT_INSTANCE_TYPE;
    private final static String DEFAULT_DURATION = MosConfig.DEFAULT_DURATION;
    private final static String DEFAULT_GROUP_ID = MosConfig.DEFAULT_GROUP_ID;

    private final static long DEFAULT_GATWAY_TIMEOUT = MosConfig.DEFAULT_TIMEOUT_BIND_GATWAY_CHECK;

    private final static int DEFAULT_TIMEOUT = MosConfig.DEFAULT_HTTP_TIMEOUT; // request timeout in seconds

    private final Client client;

    private final List<ImageTemplate> imageTemplates = new LinkedList<>();

    private final List<Zone> zones = new LinkedList<>();

    public MosClient(String key, String secret) throws Throwable {
        client = new Client(key, secret, DEFAULT_API_URL, DEFAULT_REGION, null, DEFAULT_TIMEOUT,
            false);

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
     * Find mos instance by name or id
     *
     * @param isNameOrId true for find by name, false for find by id
     */
    public MosInstance find(String pattern, boolean isNameOrId) {
        JSONObject response = null;
        try {
            if (isNameOrId) {
                response = client.DescribeInstances(null, new String[]{pattern}, 100, 0, null);
            } else {
                response = client.DescribeInstances(new String[]{pattern}, null, 100, 0, null);
            }

            Object jsonInstanceSet = response.getJSONObject("DescribeInstancesResponse").get("InstanceSet");

            // empty instance will return empty string for in instance set
            if (jsonInstanceSet instanceof String) {
                return null;
            }

            if (jsonInstanceSet instanceof JSONObject) {
                JSONObject jsonSet = (JSONObject) jsonInstanceSet;
                JSONObject jsonObject = jsonSet.getJSONObject("Instance");
                return GSON.fromJson(jsonObject.toString(), MosInstance.class);
            }

            return null;

        } catch (JSONException e) {
            MosException mosException = new MosException("DescribeInstances: Wrong response data", e);
            mosException.setError(response);
            throw mosException;
        } catch (Throwable e) {
            throw new MosException("DescribeInstances: Exception from request", e);
        }
    }

    /**
     * Instance list
     *
     * @return instance list
     */
    public List<MosInstance> listInstance() {
        JSONObject response = null;
        List<MosInstance> list = null;
        Object rawInstances = null;

        try {
            response = client.DescribeInstances(null, null, 100, 0, null);
            Object jsonInstanceSet = response.getJSONObject("DescribeInstancesResponse").get("InstanceSet");

            // empty instance will return empty string for in instance set
            if (jsonInstanceSet instanceof String) {
                return new ArrayList<>(0);
            }

            // has instance if type is JSONObject
            if (jsonInstanceSet instanceof JSONObject) {
                try {
                    JSONObject jsonSet = (JSONObject) jsonInstanceSet;
                    rawInstances = jsonSet.get("Instance");
                } catch (JSONException e) {
                    // cannot find instance data since data is out of range
                    MosSizeInfo sizeInfo = GSON.fromJson(jsonInstanceSet.toString(), MosSizeInfo.class);
                    return new ArrayList<>(0);
                }

                // only one instance
                if (rawInstances instanceof JSONObject) {
                    JSONObject jsonObject = (JSONObject) rawInstances;
                    MosInstance instance = GSON.fromJson(jsonObject.toString(), MosInstance.class);

                    list = new ArrayList<>(1);
                    list.add(instance);
                    return list;
                }
            }

            if (rawInstances instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) rawInstances;
                list = new ArrayList<>(jsonArray.length());

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonItem = jsonArray.getJSONObject(i);
                    MosInstance instance = GSON.fromJson(jsonItem.toString(), MosInstance.class);
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

    public MosInstance createInstance(String imageName, String instanceName) {
        ImageTemplate template = getImageTemplate(imageName);

        MosInstance instance = null;
        JSONObject result = null;
        try {
            result = client.CreateInstance(
                template.getTemplateId(),
                DEFAULT_INSTANCE_TYPE,
                DEFAULT_SSH_KEY_NAME,
                0,
                0,
                null,
                DEFAULT_DURATION,
                instanceName,
                DEFAULT_ZONE_ID,
                DEFAULT_GROUP_ID
            );

            JSONObject jsonObject = result.getJSONObject("CreateInstanceResponse").getJSONObject("Instance");
            instance = GSON.fromJson(jsonObject.toString(), MosInstance.class);
            if (instance == null || instance.getId() == null) {
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
            if (!bindNatGateway(instance.getId())) {
                String msg = String
                    .format("Fail to bind nat gateway for instance: %s, return false", instance.getId());

                throw new MosException(msg, null, instance);
            }

            // reload instance with ip address
            return find(instance.getId(), false);

        } catch (Throwable e) {
            throw new MosException(e.getMessage(), e, instance);
        }
    }

    public boolean bindNatGateway(String instanceId) {
        JSONObject result = null;
        try {
            // wait mos instance running
            this.instanceStatusSync(instanceId, MosInstance.STATUS_RUNNING, DEFAULT_GATWAY_TIMEOUT);

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
     * @param timeout in millseconds
     * @return boolean is status loaded or not
     */
    public boolean instanceStatusSync(final String instanceId, final String expectStatus,
        final long timeout) {
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
