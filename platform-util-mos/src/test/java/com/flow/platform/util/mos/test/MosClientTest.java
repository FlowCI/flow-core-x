package com.flow.platform.util.mos.test;

import com.flow.platform.util.mos.*;
import org.junit.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by gy@fir.im on 01/06/2017.
 * Copyright fir.im
 */
public class MosClientTest {

    private final static String TEST_KEY = "2b187cb883944507b77fd5ec2e011032";
    private final static String TEST_SECRET = "ca58cfe0b699424cb5b29e37cf1ce7cf";

    private static MosClient client;

    private final List<Instance> instanceList = new LinkedList<>();

    @BeforeClass
    public static void beforeClass() throws Throwable {
        client = new MosClient(TEST_KEY, TEST_SECRET);
    }

    @Before
    public void before() {
        instanceList.clear();
    }

    @Test
    public void should_get_images() throws Exception {
        // check all templates is loaded
        List<ImageTemplate> templates = client.getImageTemplates();
        Assert.assertTrue(templates.size() > 0);

        // check load templates by name
        ImageTemplate template = client.getImageTemplate("flow-osx-0992-ebs-bj3");
        Assert.assertNotNull(template);

        Assert.assertNotNull(template.getChecksum());
        Assert.assertNotNull(template.getSize());
        Assert.assertNotNull(template.getTemplateId());
        Assert.assertNotNull(template.getPublic());
        Assert.assertNotNull(template.getStatus());
    }

    @Test
    public void should_get_available_zones() throws Exception {
        List<Zone> zones = client.getZones();
        Assert.assertNotNull(zones);
        Assert.assertEquals(4, zones.size()); // keep track mos zone change
    }

    @Test
    public void should_get_nat_gateway() throws Exception {
        List<NatGateway> gateways = client.getNatGateWay();
        Assert.assertNotNull(gateways);
        Assert.assertTrue(gateways.size() >= 0);
    }

    @Test
    public void should_list_instances() {
        List<Instance> instances = client.listInstance();
        Assert.assertNotNull(instances);
        Assert.assertTrue(instances.size() > 0);
    }

    @Ignore
    @Test
    public void should_create_instance_and_load_status_in_sync() throws InterruptedException {
        // when: create and start instance
        Instance instance = null;

        try {
            instance = client.createInstance("flow-osx-83-109-bj4", "flow-platform-test-01");
            Assert.assertNotNull(instance.getInstanceId());
            Assert.assertEquals("init", instance.getStatus());
            instanceList.add(instance);

            // then: wait instance status to running
            Assert.assertEquals(true,
                    client.instanceStatusSync(instance.getInstanceId(), Instance.STATUS_RUNNING, 1000 * 30));
        } catch (MosException e) {
            if (e.getInstance() != null) {
                instanceList.add(e.getInstance());
            }
        }
    }

    @After
    public void after() {
        for (Instance instance : instanceList) {
            client.instanceStatusSync(instance.getInstanceId(), Instance.STATUS_RUNNING, 1000 * 30);
            client.deleteInstance(instance.getInstanceId());
        }
    }
}