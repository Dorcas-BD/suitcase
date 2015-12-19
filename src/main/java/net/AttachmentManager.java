package net;

import model.AggregateTableInfo;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.wink.client.WinkClient;
import utils.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//!!!ATTENTION!!! One AttachmentManager per table
public class AttachmentManager {
    private AggregateTableInfo table;
    private Map<String, Map<String, String>> allAttachments;
    private Map<String, Boolean> hasManifestMap;
    private WinkClient wc;

    public AttachmentManager(AggregateTableInfo table, WinkClient wc) {
        if (table.getSchemaETag() == null) {
            throw new IllegalStateException("SchemaETag has not been set!");
        }

        this.table = table;
        this.wc = wc;
        this.allAttachments = new HashMap<String, Map<String, String>>();
        this.hasManifestMap = new HashMap<String, Boolean>();
    }

    public void getListOfRowAttachments(String rowId) throws Exception {
        //TODO: download manifest for multiple rows in parallel

        if (!this.allAttachments.containsKey(rowId)) {
            Map<String, String> attachmentsMap = new HashMap<String, String>();
            this.hasManifestMap.put(rowId, true);

            try {
                JSONArray attachments = wc.getManifestForRow(
                        this.table.getServerUrl(),
                        this.table.getAppId(),
                        this.table.getTableId(),
                        this.table.getSchemaETag(),
                        rowId).getJSONArray("files");

                for (int i = 0; i < attachments.size(); i++) {
                    JSONObject attachmentJson = attachments.getJSONObject(i);
                    attachmentsMap.put(attachmentJson.optString("filename"), attachmentJson.optString("downloadUrl"));
                }
            } catch (Exception e) {
                System.out.println("Attachments Manifest Missing!");
                this.hasManifestMap.put(rowId, false);
            }

            this.allAttachments.put(rowId, attachmentsMap);
        }
    }

    public URL getAttachmentUrl(String rowId, String filename, boolean localUrl) throws IOException {
        if (!this.allAttachments.containsKey(rowId)) {
            throw new IllegalStateException("Row manifest has not been downloaded");
        }
        if (!this.allAttachments.get(rowId).containsKey(filename)) {
            System.out.println(filename);
            throw new IllegalArgumentException("Filename not found");
        }

        if (!this.hasManifestMap.get(rowId)) {
            return null;
        }

        if (localUrl) {
            return new URL("file:///" + getAttachmentLocalPath(rowId, filename));
        } else {
            return new URL(this.allAttachments.get(rowId).get(filename));
        }
    }

    public void downloadAttachments(String rowId, boolean scanRawJsonOnly) throws IOException {
        //TODO: implement multithreaded download
        if (!this.allAttachments.containsKey(rowId)) {
            throw new IllegalStateException("Row manifest has not been downloaded");
        }

        if (this.hasManifestMap.get(rowId)) {
            if (scanRawJsonOnly) {
                downloadFile(rowId, getJsonFilename(rowId));
            } else {
                for (String s : this.allAttachments.get(rowId).keySet()) {
                    downloadFile(rowId, s);
                }
            }
        }
    }

    public InputStream getScanRawJsonStream(String rowId) throws IOException {
        //TODO: make sure file has been downloaded

        if (!this.hasManifestMap.get(rowId)) {
            return null;
        }

        return Files.newInputStream(getAttachmentLocalPath(rowId, getJsonFilename(rowId)));
    }

    private Path getAttachmentLocalPath(String rowId, String filename) throws IOException {
        //Warning: Doesn't check if filename is valid

        String sanitizedRowId = WinkClient.convertRowIdForInstances(rowId);

        if (Files.notExists(Paths.get(FileUtils.getInstancesPath(table).toString(), sanitizedRowId))) {
            Files.createDirectory(Paths.get(FileUtils.getInstancesPath(table).toString(), sanitizedRowId));
        }

        return Paths.get(
                FileUtils.getInstancesPath(this.table).toString(),
                sanitizedRowId, filename
        ).toAbsolutePath();
    }

    private String getJsonFilename(String rowId) {
        return "raw_" + WinkClient.convertRowIdForInstances(rowId) + ".json";
    }

    private void downloadFile(String rowId, String filename) throws IOException {
        Path savePath = getAttachmentLocalPath(rowId, filename);
        Files.deleteIfExists(savePath); //TODO: ask in UI

        try {
            InputStream in = getAttachmentUrl(rowId, filename, false).openStream();
            Files.copy(in, savePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
