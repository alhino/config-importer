package org.ovirt;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import org.ovirt.engine.sdk.Api;
import org.ovirt.engine.sdk.entities.CPU;
import org.ovirt.engine.sdk.entities.Cluster;
import org.ovirt.engine.sdk.entities.DataCenter;
import org.ovirt.engine.sdk.entities.Host;
import org.ovirt.engine.sdk.entities.Storage;
import org.ovirt.engine.sdk.entities.StorageDomain;
import org.ovirt.engine.sdk.entities.Version;
import org.ovirt.engine.sdk.exceptions.ServerException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by ahino on 9/11/15.
 */
public class ConfigImporterNg {

    private static final String CONFIG_FILE = "/home/ahino/src/weekatons/config-importer/src/main/resources/config.xml";

    private Api api;
    private Document doc;

    public ConfigImporterNg() {
        try {
            SAXBuilder builder = new SAXBuilder();
            doc = builder.build(new File(CONFIG_FILE));
            api = new Api(doc.getRootElement().getAttributeValue("engineUrl"),
                          doc.getRootElement().getAttributeValue("admin"),
                          doc.getRootElement().getAttributeValue("password"));
            System.out.println("@@@ Welcome to oVirt Config Importer");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processConfigFile() {
        try {
            List<Element> dcs = doc.getRootElement().getChildren("DataCenter");
            dcs.stream()
               .filter(dcNotInDB)
               .map(convertDCXmlToDCObject)
               .forEach(addDC);

            // add dc clusters
            List<Element> dcClusters = doc.getRootElement().getChildren("Cluster");
            dcClusters.stream()
                      .filter(clusterNotInDB)
                      .map(convertClusterXmlToClusterObject)
                      .forEach(addCluster);


            // add cluster hosts
            List<Element> hosts = doc.getRootElement().getChildren("Host");
            hosts.stream()
                 .filter(hostNotInDB)
                 .map(convertHostXmlToHostObject)
                 .forEach(addHost);


            // add nfs sds
            List<Element> sds = doc.getRootElement().getChildren("StorageDomain");
            sds.stream()
                    .filter(sdNotInDB)
                    .filter(sd -> sd.getAttributeValue("type").equals("nfs"))
                    .map(convertNfsSDXmlToSDObject)
//                    .peek(sd -> sd.setDataCenter(pair.getFirst().getCluster().getDataCenter()))
//                    .peek(sd -> sd.setHost(pair.getFirst()))
//                    .map(sd -> new Pair<>(pair.getFirst().getCluster().getDataCenter(), sd))
                    .filter(sd -> sd != null)
                    .forEach(addSD);

            // add iscsi sds
            sds.stream()
                    .filter(sdNotInDB)
                    .filter(sd -> sd.getAttributeValue("type").equals("iscsi"))
                    .map(convertIscsiSDXmlToSDObject)
//                    .peek(sd -> sd.setDataCenter(pair.getFirst().getCluster().getDataCenter()))
//                    .peek(sd -> sd.setHost(pair.getFirst()))
//                    .map(sd -> new Pair<>(pair.getFirst().getCluster().getDataCenter(), sd))
                    .forEach(addSD);

            // add glusterfs sds
            sds.stream()
                    .filter(sdNotInDB)
                    .filter(sd -> sd.getAttributeValue("type").equals("glusterfs"))
                    .map(convertGlusterfsSDXmlToSDObject)
//                    .peek(sd -> sd.setDataCenter(pair.getFirst().getCluster().getDataCenter()))
//                    .peek(sd -> sd.setHost(pair.getFirst()))
//                    .map(sd -> new Pair<>(pair.getFirst().getCluster().getDataCenter(), sd))
                    .forEach(addSD);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Predicate<Element> dcNotInDB =
            e -> {
                try {
                    return api.getDataCenters().get(e.getAttributeValue("name")) == null;
                } catch (Exception e1) {
                    e1.printStackTrace();
                    return false;
                }
            };

    private Function<Element, DataCenter> convertDCXmlToDCObject =
            element -> {
                DataCenter dc = new DataCenter();
                dc.setLocal(false);
                dc.setName(element.getAttributeValue("name"));
                dc.setDescription(element.getAttributeValue("description"));
                dc.setComment(element.getAttributeValue("comment"));
                Version version = new Version();
                version.setFullVersion(element.getChildText("CompatibilityVersion"));
                dc.setVersion(version);
                return dc;
            };

    private Predicate<Element> clusterNotInDB =
            e -> {
                try {
                    return api.getClusters().get(e.getAttributeValue("name")) == null;
                } catch (Exception e1) {
                    e1.printStackTrace();
                    return false;
                }
            };

    private Function<Element, Cluster> convertClusterXmlToClusterObject =
            element -> {
                Cluster clusterToAdd = new Cluster();
                clusterToAdd.setName(element.getAttributeValue("name"));
                clusterToAdd.setDescription(element.getAttributeValue("description"));
                clusterToAdd.setComment(element.getAttributeValue("comment"));
                if (element.getChild("General") != null) {
                    CPU cpu = new CPU();
                    cpu.setArchitecture(element.getChild("General").getChildText("CpuArchitecture"));
                    cpu.setId(element.getChild("General").getChildText("CpuType"));
                    clusterToAdd.setCpu(cpu);
                }
                String dcName = element.getAttributeValue("datacenter");
                try {
                    clusterToAdd.setDataCenter(api.getDataCenters().get(dcName));
                } catch (ServerException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return clusterToAdd;
            };

    private Function<Element, org.ovirt.engine.sdk.entities.Host> convertHostXmlToHostObject =
            element -> {
                org.ovirt.engine.sdk.entities.Host hostToAdd = new org.ovirt.engine.sdk.entities.Host();
                hostToAdd.setName(element.getAttributeValue("name"));
                hostToAdd.setDescription(element.getAttributeValue("description"));
                hostToAdd.setAddress(element.getAttributeValue("address"));
                hostToAdd.setRootPassword(element.getChild("Authentication").getAttributeValue("password"));
                String clusterName = element.getAttributeValue("cluster");
                try {
                    hostToAdd.setCluster(api.getClusters().get(clusterName));
                } catch (ServerException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return hostToAdd;
            };

    private Function<Element, StorageDomain>  convertNfsSDXmlToSDObject  =
            e -> {
                StorageDomain sdToAdd = new StorageDomain();
                sdToAdd.setName(e.getAttributeValue("name"));
                sdToAdd.setDescription(e.getAttributeValue("description"));
                sdToAdd.setComment(e.getAttributeValue("comment"));
                sdToAdd.setType(e.getAttributeValue("domainFunction"));
                Host host = null;
                try {
                    host = api.getHosts().get(e.getAttributeValue("host"));
                } catch (ServerException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                if (!host.getStatus().getState().equals("up")) {
                    System.out.println("@@@ Host '"+host.getName()+"' is not up, SD '"+sdToAdd.getName()+"' will cannot be added");
                    return null;
                }
                sdToAdd.setHost(host);
                try {
                    sdToAdd.setDataCenter(api.getDataCenters().get(e.getAttributeValue("datacenter")));
                } catch (ServerException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                Storage storage = new Storage();
                storage.setAddress(e.getAttributeValue("server"));
                storage.setPath(e.getAttributeValue("exportPath"));
                storage.setType("nfs");
                String format = "V3";
                if (e.getAttributeValue("format") != null && !e.getAttributeValue("format").isEmpty()) {
                    format = e.getAttributeValue("format");
                }
                sdToAdd.setStorageFormat(format);
                sdToAdd.setStorage(storage);
                return sdToAdd;
            };

    private Function<Element, StorageDomain> convertIscsiSDXmlToSDObject =
            e -> {
                StorageDomain sdToAdd = new StorageDomain();
                sdToAdd.setName(e.getAttributeValue("name"));
                sdToAdd.setDescription(e.getAttributeValue("description"));
                sdToAdd.setComment(e.getAttributeValue("comment"));
                sdToAdd.setType(e.getAttributeValue("domainFunction"));
                return sdToAdd;
            };

    private Function<Element, StorageDomain> convertGlusterfsSDXmlToSDObject =
            e -> {
                StorageDomain sdToAdd = new StorageDomain();
                sdToAdd.setName(e.getAttributeValue("name"));
                sdToAdd.setDescription(e.getAttributeValue("description"));
                sdToAdd.setComment(e.getAttributeValue("comment"));
                sdToAdd.setType(e.getAttributeValue("domainFunction"));
                return sdToAdd;
            };

    private Predicate<Element> hostNotInDB =
            e -> {
                try {
                    return api.getHosts().get(e.getAttributeValue("name")) == null;
                } catch (Exception e1) {
                    e1.printStackTrace();
                    return false;
                }
            };

    private Predicate<Element> sdNotInDB =
            e -> {
                try {
                    return api.getStorageDomains().get(e.getAttributeValue("name")) == null;
                } catch (Exception e1) {
                    e1.printStackTrace();
                    return false;
                }
            };

    private Consumer<StorageDomain> addSD =
            sd -> {
                try {
                    System.out.println("@@@ Adding SD '"+sd.getName()+"' ...");
                    api.getStorageDomains().add(sd);
                    System.out.println("@@@ SD '" + sd.getName() + "' added");
                    System.out.println("@@@ Attaching SD to DC");
                    api.getDataCenters().get(sd.getDataCenter().getName()).getStorageDomains().add(sd);
                    System.out.println("@@@ Waiting to activate SD");
                    while (!api.getDataCenters().get(sd.getDataCenter().getName()).getStorageDomains().get(sd.getName()).getStatus().getState().equals("active")) {
                        System.out.println("@@@ SD status is: "+api.getDataCenters().get(sd.getDataCenter().getName()).getStorageDomains().get(sd.getName()).getStatus().getState());
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("@@@ SD status is up");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            };

    private Consumer<org.ovirt.engine.sdk.entities.Host> addHost =
            host -> {
                try {
                    System.out.println("@@@ Adding Host '" + host.getName() + "' ...");
                    api.getHosts().add(host);
                    System.out.println("@@@ Waiting for '"+host.getName()+"' to go up ...");
                    while (!api.getHosts().get(host.getName()).getStatus().getState().equals("up")) {
                        System.out.println("@@@ host status is: " + api.getHosts().get(host.getName()).getStatus().getState());
                        if (api.getHosts().get(host.getName()).getStatus().getState().equals("install_failed")) {
                            return;
                        }
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("@@@ host status is up");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };

    private Consumer<Cluster> addCluster =
            cluster -> {
                try {
                    System.out.println("@@@ Creating Cluster '"+cluster.getName()+"' ...");
                    api.getClusters().add(cluster);
                    System.out.println("@@@ Cluster '" + cluster.getName() + "' created");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };

    private Consumer<DataCenter> addDC =
            dc -> {
                try {
                    System.out.println("@@@ Creating DC '" + dc.getName() + "' ...");
                    api.getDataCenters().add(dc);
                    System.out.println("@@@ DC '" + dc.getName() + "' created");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };

    public static void main(String[] args) {
        ConfigImporterNg ci = new ConfigImporterNg();
        ci.processConfigFile();
        System.exit(0);
    }
}
