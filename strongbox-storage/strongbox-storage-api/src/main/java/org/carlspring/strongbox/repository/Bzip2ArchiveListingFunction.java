package org.carlspring.strongbox.repository;

import org.carlspring.strongbox.providers.io.RepositoryPath;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * @author Przemyslaw Fusik
 */
public enum Bzip2ArchiveListingFunction
        implements ArchiveListingFunction
{

    INSTANCE;

    @Override
    public Set<String> listFilenames(final RepositoryPath path)
            throws IOException
    {
        try (InputStream is = Files.newInputStream(path);
             BufferedInputStream bis = new BufferedInputStream(is);
             BZip2CompressorInputStream bzIs = new BZip2CompressorInputStream(bis);
             ArchiveInputStream tarIs = new TarArchiveInputStream(bzIs))
        {
            return getEntriesNames(tarIs);
        }
    }

    @Override
    public boolean supports(final RepositoryPath path)
    {
        return path.getFileName().toString().endsWith(".tar.bz2");
    }
}
