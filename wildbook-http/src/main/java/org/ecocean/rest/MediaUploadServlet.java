package org.ecocean.rest;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.ecocean.media.AssetStore;
import org.ecocean.media.MediaAsset;
import org.ecocean.media.MediaAssetFactory;
import org.ecocean.mmutil.MediaUtilities;
import org.ecocean.servlet.ServletUtils;
import org.ecocean.util.FileUtilities;
import org.ecocean.util.LogBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsix.database.ConnectionInfo;
import com.samsix.database.Database;
import com.samsix.database.DatabaseException;
import com.samsix.database.SqlInsertFormatter;
import com.samsix.util.OsUtils;

@MultipartConfig
public class MediaUploadServlet
    extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(MediaUploadServlet.class);

    private static WeakHashMap<String, FileSet> filesMap = new WeakHashMap<>();

    public static void clearFileSet(final String msid)
    {
        filesMap.remove(String.valueOf(msid));
    }

    public static void deleteFileFromSet(final HttpServletRequest request,
                                         final int msid,
                                         final String filename) throws DatabaseException, IOException
    {
        if (logger.isDebugEnabled()) {
            logger.debug("About to delete [" + filename + "] from submission [" + msid + "]");
        }

        FileSet fileSet = filesMap.get(String.valueOf(msid));

        if (fileSet == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Fileset not found in [" + filesMap.values()+ "]");
            }
            return;
        }

        try (Database db = ServletUtils.getDb(request)) {
            for (FileMeta file : new ArrayList<FileMeta>(fileSet.files)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Checking against [" + file.name + "]...");
                }
                if (file.name.equalsIgnoreCase(filename)) {
                    //
                    // WARNING: If the file has been saved (the thread actually got run) then
                    // we can delete the file. If not, we will remove it from the user's
                    // browser list but the mediasubmission will still contain this.
                    //
                    if (file.getId() != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Deleting from file system.");
                        }
                        MediaAssetFactory.deleteMedia(db, msid, file.getId());
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Removing from fileset.");
                    }
                    fileSet.files.remove(file);
                }
            }
        }
    }


    /***************************************************
     * URL: /mediaupload
     * doPost(): upload the files and other parameters
     ****************************************************/
    @Override
    protected void doPost(final HttpServletRequest request,
                          final HttpServletResponse response)
        throws ServletException, IOException
    {
        // Upload File Using Apache FileUpload
        FileSet upload = uploadByApacheFileUpload(request);

        if (logger.isDebugEnabled()) {
            logger.debug(LogBuilder.quickLog("upload", upload.toString()));
        }
        FileSet fileset = filesMap.get(upload.getID());

        if (fileset == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Adding new fileset...");
            }
            fileset = upload;
            filesMap.put(upload.getID(), fileset);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug(LogBuilder.quickLog("Found fileset", fileset.toString()));
                logger.debug(LogBuilder.quickLog("Number of new files added", upload.getFiles().size()));
            }
            fileset.getFiles().addAll(upload.getFiles());
        }

        if (logger.isDebugEnabled()) {
            logger.debug(LogBuilder.quickLog("Current fileset", fileset.toString()));
        }

        // 2. Set response type to json
        response.setContentType("application/json");

        //
        // TODO: Add configuration to control which domains you want here so
        // that we don't have to do *.
        // UPDATE: This was replaced with a CorsFilter in the web.xml file.
        //
//        response.setHeader("Access-Control-Allow-Origin", "*");

        // 3. Convert List<FileMeta> into JSON format
        ObjectMapper mapper = new ObjectMapper();

        // 4. Send resutl to client
        mapper.writeValue(response.getOutputStream(), upload);
    }


    /***************************************************
     * URL: /upload?f=value
     * doGet(): get file of index "f" from List<FileMeta> as an attachment
     ****************************************************/
    @Override
    protected void doGet(final HttpServletRequest request,
                         final HttpServletResponse response)
        throws ServletException, IOException
    {
         // 1. Get f from URL upload?f="?"
         String value = request.getParameter("f");

         if (value == null) {
             return;
         }

         String id = request.getParameter("id");

         if (id == null) {
             return;
         }

         FileSet fileset = filesMap.get(id);

         // 2. Get the file of index "f" from the list "files"
         FileMeta getFile = fileset.getFiles().get(Integer.parseInt(value));

         if (logger.isDebugEnabled()) {
             logger.debug(new LogBuilder("GET").appendVar("id", value).appendVar("file", getFile).toString());
         }

         try {
             // 3. Set the response content type = file content type
             response.setContentType(getFile.getType());

             // 4. Set header Content-disposition
             response.setHeader("Content-disposition", "attachment; filename=\"" + getFile.getName() + "\"");

             // 5. Copy file inputstream to response outputstream
             InputStream input = getFile.getContent();
             OutputStream output = response.getOutputStream();
             byte[] buffer = new byte[1024*10];

             for (int length = 0; (length = input.read(buffer)) > 0;) {
                 output.write(buffer, 0, length);
             }

             output.close();
             input.close();
         } catch (IOException e) {
             e.printStackTrace();
         }
    }

    private FileSet uploadByApacheFileUpload(final HttpServletRequest request)
        throws IOException, ServletException
    {
        //System.out.println("calling upload");
        FileSet fileset = new FileSet();

        // 1. Check request has multipart content
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        FileMeta temp = null;

        // 2. If yes (it has multipart "files")
        if (isMultipart) {

            // 2.1 instantiate Apache FileUpload classes
            DiskFileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);

            // 2.2 Parse the request
            try {
                // 2.3 Get all uploaded FileItem
                List<FileItem> items = upload.parseRequest(request);

                if (items.isEmpty()) {
                    logger.warn("Items are empty!");
                    return fileset;
                }

                // 2.4 Go over each FileItem
                for(FileItem item:items) {

                    // 2.5 if FileItem is not of type "file"
                    if (item.isFormField())  {
                        switch (item.getFieldName()) {
                            case "mediaid":
                                fileset.setID(item.getString());
                                break;
                            case "submitterid":
                                fileset.setSubmitter(item.getString());
                                break;
                        }
                    } else {
                        // 2.7 Create FileMeta object
                        temp = new FileMeta();
                        temp.setName(item.getName());
                        temp.setContent(item.getInputStream());
                        temp.setType(item.getContentType());
                        temp.setSize(item.getSize());

                        // 2.7 Add created FileMeta object to List<FileMeta> files
                        fileset.getFiles().add(temp);
                    }
                }

//                File baseDir = new File("/mediasubmission", fileset.getID());

                //
                // TODO: Have a way to specify the asset store for media submissions
                // For now I will assume the default one.
                //
                AssetStore store = AssetStore.getDefault();

                int id;
                try {
                    id = Integer.parseInt(fileset.getID());
                } catch(NumberFormatException ex) {
                    logger.error("Can't parse id [" + fileset.getID() + "]", ex);
                    id = -1;
                }

                for (FileMeta file : fileset.getFiles()) {
                    //
                    // TODO: Fix this. We have to set the URL to *something* or else the button on the submission
                    // form does not turn to Delete but rather stays Cancel. So for now, I'm just putting a dummy
                    // value here for the url. I had changed this because we no longer know the filename of the
                    // saved file anyway. We need the delete to be the id or some such of the resulting media.
                    // Requires sockets to interact properly.
                    //
//                  file.setUrl(store.webPath(new File(baseDir, file.getName()).toPath()).toExternalForm());
                    file.setUrl("/nothing.jpg");

                    if (MediaUtilities.isImageFile(file.getName())) {
//                        file.setThumbnailUrl("/" + getThumbnailFile(baseDir, file.getName()));
                        file.setThumbnailUrl("/images/upload_small.png");
                    } else if (MediaUtilities.isGpsFile(file.getName())) {
                        file.setThumbnailUrl("/images/map-icon.png");
                    } else if (MediaUtilities.isVideoFile(file.getName())) {
                        file.setThumbnailUrl("/images/video_thumb.jpg");
                    } else {
                        file.setThumbnailUrl("/images/upload_small.png");
                    }

                    //
                    // Shell out the other stuff to a thread. The asynchronous nature
                    // of this may make it so that thumbnails are not actually created
                    // yet by the time the user gets the results back from this servlet.
                    // If so, they will get a broken link image.
                    //
                    Integer submitterId = null;
                    if (! StringUtils.isBlank(fileset.getSubmitter())) {
                        submitterId = Integer.parseInt(fileset.getSubmitter());
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("About to save file [" + file.getName() + "]");
                    }

                    MediaAsset ma;
                    try {
                        ma = saveMedia(ServletUtils.getConnectionInfo(request),
                                       store,
                                       id,
                                       submitterId,
                                       file);
                        if (ma != null) {
                            file.setMetaTimestamp(ma.getMetaTimestamp());
                            file.setMetaLatitude(ma.getMetaLatitude());
                            file.setMetaLongitude(ma.getMetaLongitude());
                        }
                    } catch (DatabaseException ex) {
                        String msg = "Can't save media.";
                        logger.error(msg, ex);
                        throw new IOException(msg, ex);
                    }
                }
            } catch (FileUploadException ex) {
                ex.printStackTrace();
            }
        }
        return fileset;
    }

//    // this method is used to get file name out of request headers
//    //
//    private static String getFilename(Part part)
//    {
//        for (String cd : part.getHeader("content-disposition").split(";")) {
//            if (cd.trim().startsWith("filename")) {
//                String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
//                return filename.substring(filename.lastIndexOf('/') + 1).substring(filename.lastIndexOf('\\') + 1); // MSIE fix.
//            }
//        }
//        return null;
//    }

    private MediaAsset saveMedia(final ConnectionInfo ci,
                                 final AssetStore store,
                                 final int submissionId,
                                 final Integer submitterId,
                                 final FileMeta file) throws DatabaseException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug(LogBuilder.get("Saving media").appendVar("submissionid", submissionId)
                         .appendVar("file.getName()", file.getName()).toString());
        }

        if (! file.getName().toLowerCase().endsWith(".zip")) {
            return processFile(ci,
                               file.getName(),
                               store,
                               submitterId,
                               submissionId,
                               file,
                               file.getContent(),
                               false);
        }

        //
        // Run zip file extraction
        //
        try (ZipInputStream   zis = new ZipInputStream(file.content)) {
            ZipEntry zipEntry;
            while ( ( zipEntry = zis.getNextEntry() ) != null )
            {
                if (logger.isDebugEnabled()) {
                    logger.debug(LogBuilder.get().appendVar("zipEntry.getName()",
                                                            zipEntry.getName()).toString());
                }

                if (zipEntry.isDirectory()) {
                    continue;
                }

                //
                // TODO: Make it so that we preserve the zip file contents?
                // In case they have the same file name in each subdir?
                // But then how are thumbs and mid-size images referenced?
                // Aren't the filenames generated from the file name of the full size
                // image? If so, would we need a thumb and mid directory in each subdir?
                // For now, let's just expand all files into base dir.
                //
                // By using the Path class we will strip out any "/"'s in the name.
                //
                Path zipfile = Paths.get(zipEntry.getName());
                processFile(ci,
                            zipfile.getFileName().toString(),
                            store,
                            submitterId,
                            submissionId,
                            file,
                            zis,
                            true);
                zis.closeEntry();
            }
            return null;
        }
    }

    private MediaAsset processFile(final ConnectionInfo ci,
                                   final String fileName,
                                   final AssetStore store,
                                   final Integer submitterId,
                                   final int submissionId,
                                   final FileMeta file,
                                   final InputStream content,
                                   final boolean fromZip) throws IOException, DatabaseException
    {
        Path relFile = FileUtilities.createUUIDRelFile(OsUtils.getFileExtension(fileName));

        MediaAsset ma = MediaUtilities.saveMedia(store, relFile, fileName, content, submitterId, fromZip, null);

        saveToDB(ci, ma, submissionId, file, fromZip);

        //
        // Shell out the processing to a separate thread so the user doesn't have
        // to wait on this.
        //
        MediaUtilities.processMediaBackground(ci, ma, store, relFile, null);

        return ma;
    }

    private void saveToDB(final ConnectionInfo ci,
                          final MediaAsset ma,
                          final int submissionId,
                          final FileMeta file,
                          final boolean fromZip) throws DatabaseException {
        if (logger.isDebugEnabled()) {
            logger.debug("About to saveToDB");
        }
        try (Database db = new Database(ci)) {
            try {
                MediaAssetFactory.save(db, ma);
            } catch (Throwable ex) {
                logger.error("Huh?", ex);
                throw ex;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Done saving basic ma.");
            }

            if (! fromZip) {
                //
                // In case the user decides to delete the file they just uploaded
                // (provided this has been called by then). If it's a zip file they
                // won't be able to delete it.
                //
                file.setId(ma.getID());
            }
            SqlInsertFormatter formatter = new SqlInsertFormatter();
            formatter.append("mediasubmissionid", submissionId);
            formatter.append("mediaid", ma.getID());

            if (logger.isDebugEnabled()) {
                logger.debug(LogBuilder.quickLog("About to save link to media", ma.getID()));
            }

            db.getTable("mediasubmission_media").insertRow(formatter);
        }
    }


    //=========================
    // FileSet Class
    //=========================
    public static class FileSet
    {
        private final List<FileMeta> files = new ArrayList<FileMeta>();
        private String id;
        private String submitter;

        public String getID()
        {
            return id;
        }

        public void setID(final String id)
        {
            this.id = id;
        }

        public List<FileMeta> getFiles() {
            return files;
        }

        @Override
        public String toString()
        {
            ToStringBuilder builder = new ToStringBuilder(this);
            return builder.append("id", id)
                          .append("files", files).toString();
        }

        public String getSubmitter() {
            return submitter;
        }

        public void setSubmitter(final String submitter) {
            this.submitter = submitter;
        }
    }

    @JsonIgnoreProperties({"content"})
    public static class FileMeta
    {
        //
        // This will be the mediaasset id after the file gets saved.
        //
        private Integer id;
        private String name;
        private long size;
        private String type;
        private String url;
        private String thumbnailUrl;
        private LocalDateTime metaTimestamp;
        private Double metaLatitude;
        private Double metaLongitude;

        private InputStream content;

        public String getName() {
          return name;
        }

        public void setName(final String name) {
          this.name = name;
        }

        public long getSize() {
          return size;
        }

        public void setSize(final long size) {
          this.size = size;
        }

        public String getType() {
          return type;
        }

        public void setType(final String type) {
          this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(final String url) {
            this.url = url;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }

        public void setThumbnailUrl(final String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
        }

        public InputStream getContent()
        {
          return content;
        }

        public void setContent(final InputStream content)
        {
          this.content = content;
        }

        public Integer getId() {
            return id;
        }

        public void setId(final Integer id) {
            this.id = id;
        }

        @Override
        public String toString()
        {
            return name;
        }

        public LocalDateTime getMetaTimestamp() {
            return metaTimestamp;
        }

        public void setMetaTimestamp(final LocalDateTime metaTimestamp) {
            this.metaTimestamp = metaTimestamp;
        }

        public Double getMetaLatitude() {
            return metaLatitude;
        }

        public void setMetaLatitude(final Double metaLatitude) {
            this.metaLatitude = metaLatitude;
        }

        public Double getMetaLongitude() {
            return metaLongitude;
        }

        public void setMetaLongitude(final Double metaLongitude) {
            this.metaLongitude = metaLongitude;
        }
    }
}
