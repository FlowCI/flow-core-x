package com.flow.platform.util.mos;

import com.google.gson.Gson;
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

    private final static Gson GSON = new Gson();

    private final static String DEFAULT_API_URL = "https://mosapi.meituan.com/mcs/v1";
    private final static String DEFAULT_REGION = "Beijing";

    private final static String DEFAULT_NET_ID = "2a9e0312-2e85-47d5-ab86-776861bd84bc";
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

        // bind net
        if (!bindNatGateway(instance.getInstanceId())) {
            String msg = String.format("Fail to bind nat gateway for instance: %s, return false",
                    instance.getInstanceId());

            // force to terminate instance if bind nat gateway failure
            deleteInstance(instance.getInstanceId());

            throw new MosException(msg, null);
        }

        return instance;
    }

    public boolean bindNatGateway(String instanceId) {
        JSONObject result = null;
        try {
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
                            System.out.println(loadedStatus);

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
