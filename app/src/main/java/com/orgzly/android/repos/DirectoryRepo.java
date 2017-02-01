package com.orgzly.android.repos;

import android.net.Uri;
import android.util.Log;

import com.orgzly.android.BookName;
import com.orgzly.android.LocalStorage;
import com.orgzly.android.util.MiscUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DirectoryRepo implements Repo {
    private static final String TAG = DirectoryRepo.class.getName();

    public static final String SCHEME = "file";

    private File mDirectory;

    private final Uri repoUri;

    /**
     *
     * @param url repo url, in the format (file:/a/b/c)
     * @param wipe should files be deleted first from directory
     */
    public DirectoryRepo(String url, boolean wipe) throws IOException {
        repoUri = Uri.parse(url);

        mDirectory = new File(repoUri.getPath());

        /* Delete entire contents of directory. */
        if (wipe) {
            LocalStorage.deleteRecursive(mDirectory);
        }

        createDir(mDirectory);
    }

    private void createDir(File dir) throws IOException {
        if (! dir.isDirectory()) {
            if (! dir.mkdirs()) {
                throw new IOException("Failed creating directory " + dir);
            }
        }
    }

    @Override
    public boolean requiresConnection() {
        return false;
    }

    @Override
    public Uri getUri() {
        return repoUri;
    }

    @Override
    public List<VersionedRook> getBooks() throws IOException {
        List<VersionedRook> result = new ArrayList<>();

        File[] files = mDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return BookName.isSupportedFormatFileName(filename);
            }
        });

        if (files != null) {
            Arrays.sort(files);

            for (int i = 0; i < files.length; i++) {
                Uri uri = repoUri.buildUpon().appendPath(files[i].getName()).build();

                result.add(new VersionedRook(
                        repoUri,
                        uri,
                        String.valueOf(files[i].lastModified()),
                        files[i].lastModified()
                ));
            }

        } else {
            Log.e(TAG, "Listing files in " + mDirectory + " returned null. No storage permission?");
        }

        return result;
    }

    @Override
    public VersionedRook retrieveBook(Rook rook, File destinationFile) throws IOException {
        File sourceFile = new File(rook.getUri().getPath());

        /* "Download" the file. */
        MiscUtils.copyFile(sourceFile, destinationFile);

        String rev = String.valueOf(sourceFile.lastModified());
        long mtime = sourceFile.lastModified();

        return new VersionedRook(rook, rev, mtime);
    }

    /**
     *
     * @param path Full path where to store the file, including the file name
     */
    @Override
    public VersionedRook storeBook(File file, String path) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File " + file + " does not exist");
        }

        File destinationFile = new File(mDirectory, path);

        /* Create directory. */
        createDir(destinationFile.getParentFile());

        String content = MiscUtils.readStringFromFile(file);
        MiscUtils.writeStringToFile(content, destinationFile);

        String rev = String.valueOf(destinationFile.lastModified());
        long mtime = destinationFile.lastModified();

        Uri uri = repoUri.buildUpon().appendPath(path).build();

        return new VersionedRook(repoUri, uri, rev, mtime);
    }

    @Override
    public VersionedRook moveBook(Uri from, Uri to) throws IOException {
        File fromFile = new File(from.getPath());
        File toFile = new File(to.getPath());

        if (toFile.exists()) {
            throw new IOException("File " + toFile + " already exists");
        }

        if (! fromFile.renameTo(toFile)) {
            throw new IOException("Failed renaming " + fromFile + " to " + toFile);
        }

        String rev = String.valueOf(toFile.lastModified());
        long mtime = toFile.lastModified();

        return new VersionedRook(repoUri, to, rev, mtime);
    }

    @Override
    public void delete(String path) throws IOException {
        File file = new File(path);

        if (file.exists()) {
            if (! file.delete()) {
                throw new IOException("Failed deleting file " + path);
            }
        }
    }

    public File getDirectory() {
        return mDirectory;
    }

    @Override
    public String toString() {
        return repoUri.toString();
    }
}
