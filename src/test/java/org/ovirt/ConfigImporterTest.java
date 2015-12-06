package org.ovirt;

import org.junit.Test;
import org.ovirt.engine.sdk.decorators.Cluster;
import org.ovirt.engine.sdk.decorators.Clusters;
import org.ovirt.engine.sdk.decorators.DataCenterStorageDomain;
import org.ovirt.engine.sdk.decorators.DataCenterStorageDomains;
import org.ovirt.engine.sdk.decorators.DataCenters;
import org.ovirt.engine.sdk.decorators.DataCenter;
import org.ovirt.engine.sdk.decorators.Host;
import org.ovirt.engine.sdk.decorators.Hosts;
import org.ovirt.engine.sdk.decorators.StorageDomain;
import org.ovirt.engine.sdk.decorators.StorageDomains;
import org.ovirt.engine.sdk.entities.Action;
import org.ovirt.engine.sdk.entities.Actions;
import org.ovirt.engine.sdk.entities.Link;
import org.ovirt.engine.sdk.entities.Status;
import org.ovirt.engine.sdk.exceptions.ServerException;

import java.io.IOException;

/**
 * Created by ahino on 9/2/15.
 */
public class ConfigImporterTest {

    @Test
    public void testProcessConfigFile() {
        ConfigImporter importer = new ConfigImporter();
        importer.processConfigFile();
    }

//    @Test
    public void testReadDCs() {
        ConfigImporter importer = new ConfigImporter();
        DataCenters dcs = importer.getDCs();
        try {
            for (DataCenter dc : dcs.list()) {
                System.out.println("@@@ dc.getName: " + dc.getName());
                System.out.println("@@@ dc.getStatus().getState: "+dc.getStatus().getState());
                System.out.println("***********************");
                System.out.println("***********************");
            }
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    @Test
    public void testReadHosts() {
        ConfigImporter importer = new ConfigImporter();
        Hosts hosts = importer.getHosts();
        try {
            for (Host host : hosts.list()) {
                System.out.println("Host status: "+host.getStatus().getState());
            }
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    @Test
    public void testReadSDsFromDCs() {
        ConfigImporter importer = new ConfigImporter();
        DataCenters dcs = importer.getDCs();
        try {
            for (DataCenter dc : dcs.list()) {
                DataCenterStorageDomains dcSDs = dc.getStorageDomains();
                for (DataCenterStorageDomain dcSD : dcSDs.list()) {
                    System.out.println("@@@ dcSD.getName: " + dcSD.getName());
                    System.out.println("@@@ dcSD.getStatus: "+dcSD.getStatus());
                    if (dcSD.getStatus() != null) {
                        System.out.println("@@@ dcSD.getStatus().getState: "+dcSD.getStatus().getState());
                    }
                    System.out.println("***********************");
                    System.out.println("***********************");
                }
            }
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    @Test
    public void testReadSDs() {
        ConfigImporter importer = new ConfigImporter();
        StorageDomains sds = importer.getSDs();
        try {
            for (StorageDomain sd : sds.list()) {
                System.out.println("sd.getName: "+sd.getName());
                System.out.println("sd.getHref: "+sd.getHref());
                System.out.println("sd.getType: "+sd.getType());
                System.out.println("sd.getId: "+sd.getId());
                System.out.println("sd.getStorage().getNfsVersion: " + sd.getStorage().getNfsVersion());
                System.out.println("sd.getStorage().getPath: " + sd.getStorage().getPath());
                System.out.println("sd.getStorageFormat: " + sd.getStorageFormat());
                System.out.println("sd.getStorage().getVfsType: "+sd.getStorage().getVfsType());
                System.out.println("sd.getStorage().getType: " + sd.getStorage().getType());
                System.out.println("sd.getStorage().getAddress: " + sd.getStorage().getAddress());
                if (sd.getStatus() != null) {
                    System.out.println("sd.getStatus().getState: " + sd.getStatus().getState());
//                    if (sd.getStatus().getState().equals("unattached")) {
//                        Status status = new Status();
//                        status.setState("up");
//                        sd.setStatus(status);
//                        sd.update();
//                    }
                } else {
                    System.out.println("sd.getStatus().getState: null");
                }
                System.out.println("sd.getStorage().getNfsVersion: " + sd.getStorage().getNfsVersion());
                System.out.println("sd.getStorage().getNfsRetrans: "+sd.getStorage().getNfsRetrans());
                System.out.println("sd.getStorage().getNfsTimeo: "+sd.getStorage().getNfsTimeo());
                System.out.println("sd.getStorage().getTarget: " + sd.getStorage().getTarget());
                Actions actions = sd.getActions();
                if (actions != null) {
                    for (Link link : actions.getLinks()) {
                        System.out.println("link.getHref: " + link.getHref());
                        System.out.println("link.getRel: " + link.getRel());
                    }
                }
                System.out.println("***********************");
                System.out.println("***********************");
            }
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    @Test
//    public void testReadDCs() {
//        ConfigImporter importer = new ConfigImporter();
//        DataCenters dcs = importer.getDCs();
//        try {
//            for (DataCenter dc : dcs.list()) {
//                System.out.println("Found DC: "+dc.getName());
//            }
//        } catch (ServerException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    @Test
//    public void testReadClusters() {
//        ConfigImporter importer = new ConfigImporter();
//        Clusters clusters = importer.getClusters();
//        try {
//            for (Cluster cluster : clusters.list()) {
//                System.out.println("Found Cluster: "+cluster.getName());
//                System.out.println("Cpu arch: "+cluster.getCpu().getArchitecture());
//                System.out.println(""+cluster.getCpu().getId());
//            }
//        } catch (ServerException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
