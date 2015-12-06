package org.ovirt;

import org.jdom2.input.SAXBuilder;
import org.ovirt.engine.sdk.Api;
import org.ovirt.engine.sdk.decorators.Clusters;
import org.ovirt.engine.sdk.decorators.Hosts;
import org.ovirt.engine.sdk.decorators.StorageDomains;
import org.ovirt.engine.sdk.entities.Action;
import org.ovirt.engine.sdk.entities.CPU;
import org.ovirt.engine.sdk.entities.Cluster;
import org.ovirt.engine.sdk.entities.Host;
import org.ovirt.engine.sdk.entities.DataCenter;
import org.ovirt.engine.sdk.decorators.DataCenters;
import org.ovirt.engine.sdk.entities.Storage;
import org.ovirt.engine.sdk.entities.StorageDomain;
import org.ovirt.engine.sdk.entities.Version;
import org.ovirt.engine.sdk.exceptions.ServerException;
import org.ovirt.engine.sdk.exceptions.UnsecuredConnectionAttemptError;

import org.jdom2.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by ahino on 9/2/15.
 */
public class ConfigImporter {

    private static final String ENGINE_URL = "http://localhost:8080/ovirt-engine/api";
    private static final String ADMIN = "admin@internal";
    private static final String ADMIN_PSW = "a";

    private static final String CONFIG_FILE = "/home/ahino/src/weekatons/config-importer/src/main/resources/config.xml";

    private Api api;

    public ConfigImporter() {
        try {
            api = new Api(ENGINE_URL, ADMIN, ADMIN_PSW);
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsecuredConnectionAttemptError unsecuredConnectionAttemptError) {
            unsecuredConnectionAttemptError.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ConfigImporter runner = new ConfigImporter();
        runner.processConfigFile();
    }

    public void processConfigFile() {
        SAXBuilder builder = new SAXBuilder();
        try {
            Document doc = builder.build(new File(CONFIG_FILE));
            handleDCs(doc.getRootElement().getChild("DataCenters").getChildren("DataCenter"));
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DataCenters getDCs() {
        return api.getDataCenters();
    }

    public Clusters getClusters() {
        return api.getClusters();
    }

    public Hosts getHosts() {
        return api.getHosts();
    }

    public StorageDomains getSDs() {
        return api.getStorageDomains();
    }

    private void handleDCs(List<Element> dcs) {
        for (Element dc : dcs) {
            try {
                DataCenter dcToAdd = api.getDataCenters().get(dc.getAttributeValue("name"));
                if (dcToAdd == null) {
                    dcToAdd = new DataCenter();
                    dcToAdd.setLocal(false);
                    dcToAdd.setName(dc.getAttributeValue("name"));
                    dcToAdd.setDescription(dc.getAttributeValue("description"));
                    dcToAdd.setComment(dc.getAttributeValue("comment"));
                    Version version = new Version();
                    version.setFullVersion(dc.getChildText("CompatibilityVersion"));
                    dcToAdd.setVersion(version);
                    api.getDataCenters().add(dcToAdd);
                } else {
                    System.out.println("Datacenter '" + dc.getAttributeValue("name") + "' exists!");
                }
                handleClusters(dcToAdd, dc.getChildren("Cluster"));
            } catch (ServerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleClusters(DataCenter dc, List<Element> clusters) {
        for (Element cluster : clusters) {
            try {
                Cluster clusterToAdd = api.getClusters().get(cluster.getAttributeValue("name"));
                if (clusterToAdd == null) {
                    clusterToAdd = new Cluster();
                    clusterToAdd.setName(cluster.getAttributeValue("name"));
                    clusterToAdd.setDescription(cluster.getAttributeValue("description"));
                    clusterToAdd.setComment(cluster.getAttributeValue("comment"));
                    clusterToAdd.setDataCenter(dc);
                    if (cluster.getChild("General") != null) {
                        CPU cpu = new CPU();
                        cpu.setArchitecture(cluster.getChild("General").getChildText("CpuArchitecture"));
                        cpu.setId(cluster.getChild("General").getChildText("CpuType"));
                        clusterToAdd.setCpu(cpu);
                    }
                    api.getClusters().add(clusterToAdd);
                } else {
                    System.out.println("Cluster '" + cluster.getAttributeValue("name") + "' exists!");
                }
                handeHosts(dc, clusterToAdd, cluster.getChildren("Host"));
            } catch (ServerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handeHosts(DataCenter dc, Cluster cluster, List<Element> hosts) {
        for (Element host : hosts) {
            try {
                Host hostToAdd = api.getHosts().get(host.getAttributeValue("name"));
                if (hostToAdd == null) {
                    hostToAdd = new Host();
                    hostToAdd.setName(host.getAttributeValue("name"));
                    hostToAdd.setDescription(host.getAttributeValue("description"));
                    hostToAdd.setAddress(host.getAttributeValue("address"));
                    hostToAdd.setRootPassword(host.getChild("Authentication").getAttributeValue("password"));
                    hostToAdd.setCluster(cluster);
                    api.getHosts().add(hostToAdd);
                    System.out.println("@@@ waiting for host to go up");
                    while (!api.getHosts().get(host.getAttributeValue("name")).getStatus().getState().equals("up")) {
                        System.out.println("@@@ host status is: " + api.getHosts().get(host.getAttributeValue("name")).getStatus().getState());
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("@@@ Host is up");
                } else {
                    System.out.println("Host '" + host.getAttributeValue("name") + "' exists!");
                    if (!api.getHosts().get(host.getAttributeValue("name")).getStatus().getState().equals("up")) {
                        api.getHosts().get(host.getAttributeValue("name")).activate(new Action());
                        while (!api.getHosts().get(host.getAttributeValue("name")).getStatus().getState().equals("up")) {
                            System.out.println("@@@ host status is: " + api.getHosts().get(host.getAttributeValue("name")).getStatus().getState());
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                handleSDs(dc, hostToAdd, host.getChildren("StorageDomain"));
            } catch (ServerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleSDs(DataCenter dc, Host host, List<Element> sds) {
        for (Element e : sds) {
            if (e.getAttributeValue("type").equals("nsf")) {
                handleNfsSD(dc, host, e);
            } else if (e.getAttributeValue("type").equals("iscsi")) {
                handleIscsiSD(dc, host, e);
            } else if (e.getAttributeValue("type").equals("glusterfs")) {
                handleGlusterSD(dc, host, e);
            }
        }
    }

    private void handleNfsSD(DataCenter dc, Host host, Element sd) {
        try {
            StorageDomain sdToAdd = api.getStorageDomains().get(sd.getAttributeValue("name"));
            if (sdToAdd == null) {
                sdToAdd = new StorageDomain();
                sdToAdd.setName(sd.getAttributeValue("name"));
                sdToAdd.setDescription(sd.getAttributeValue("description"));
                sdToAdd.setComment(sd.getAttributeValue("comment"));
                sdToAdd.setType(sd.getAttributeValue("domainFunction"));
                sdToAdd.setHost(host);
                Storage storage = new Storage();
                storage.setHost(host);
                sdToAdd.setDataCenter(dc);
                storage.setAddress(sd.getAttributeValue("address"));
                storage.setPath(sd.getAttributeValue("exportPath"));
                storage.setType("nfs");
                sdToAdd.setStorage(storage);
                api.getStorageDomains().add(sdToAdd);
                api.getDataCenters().get(dc.getName()).getStorageDomains().add(sdToAdd);
                System.out.println("@@@ Waiting to activate SD");
                while (!api.getDataCenters().get(dc.getName()).getStorageDomains().get(sdToAdd.getName()).getStatus().getState().equals("active")) {
                    System.out.println("@@@ SD status is: ");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("@@@ SD is active");
                //                org.ovirt.engine.sdk.decorators.StorageDomain sdFromDb = api.getStorageDomains().get(sdToAdd.getName());
                //                Status status = new Status();
                //                status.setState("up");
                //                sdFromDb.setStatus(status);
                //                sdFromDb.update();
                //                System.out.println("@@@ waiting for sd to go up");
                //                while (!api.getStorageDomains().get(sd.getAttributeValue("name")).getStatus().getState().equals("up")) {
                //                    System.out.println("@@@ sd status: " + api.getStorageDomains().get(sd.getAttributeValue("name")).getStatus().getState());
                //                    try {
                //                        Thread.sleep(10000);
                //                    } catch (InterruptedException e) {
                //                        e.printStackTrace();
                //                    }
                //                }
            } else {
                System.out.println("Storage Domain '" + sdToAdd.getName() + "' exists!");
            }
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleIscsiSD(DataCenter dc, Host host, Element sd) {
    }

    private void handleGlusterSD(DataCenter dc, Host host, Element sd) {
    }
}
