package org.carlspring.strongbox.artifact.generator;

import org.carlspring.strongbox.artifact.coordinates.RpmArtifactCoordinates;
import org.carlspring.strongbox.util.TestFileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import org.redline_rpm.Builder;
import static org.redline_rpm.header.Architecture.NOARCH;
import static org.redline_rpm.header.Os.LINUX;
import static org.redline_rpm.header.RpmType.BINARY;


public class RpmArtifactGenerator
        implements ArtifactGenerator
{

    private RpmArtifactCoordinates coordinates;

    private Path basePath;

    private Path packagePath;


    public RpmArtifactGenerator(Path basePath)
    {
        super();
        this.basePath = basePath;
    }

    public RpmArtifactGenerator of(RpmArtifactCoordinates coordinates)
    {
        this.coordinates = coordinates;

        return this;
    }

    public RpmArtifactGenerator in(Path path)
    {
        this.basePath = path;
        return this;
    }

    public RpmArtifactCoordinates getCoordinates()
    {
        return coordinates;
    }

    public void setCoordinates(RpmArtifactCoordinates coordinates)
    {
        this.coordinates = coordinates;
    }

    public Path getBasePath()
    {
        return basePath;
    }

    public void setBasePath(Path basePath)
    {
        this.basePath = basePath;
    }

    public Path getPackagePath()
    {
        return packagePath;
    }

    public void setPackagePath(Path packagePath)
    {
        this.packagePath = packagePath;
    }

    public void generate(long byteSize)
            throws IOException
    {
        try
        {
            File fileWithRandomSize = Paths.get(basePath.toString(), getPackagePath().toString()).toFile();
            TestFileUtils.generateFile(fileWithRandomSize, byteSize);

            Builder builder = new Builder();
            builder.setPackage(coordinates.getBaseName(), coordinates.getVersion(), "1");
            builder.setBuildHost("localhost");
            // TODO: Issue #1728
            // For multiple licenses, this should be in the format: "Apache 2.0 or EPL or MIT"
            builder.setLicense("GPL");
            builder.setPlatform(NOARCH, LINUX);
            builder.setType(BINARY);
            builder.addFile("etc", fileWithRandomSize);
            builder.build(basePath.toFile());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public Path generateArtifact(URI uri,
                                 long bytesSize)
            throws IOException
    {
        RpmArtifactCoordinates coordinates = RpmArtifactCoordinates.parse(uri.toString());

        setCoordinates(coordinates);

        generate(bytesSize);

        return Paths.get(basePath.toString(), coordinates.toPath());
    }

    @Override
    public Path generateArtifact(String id,
                                 String version,
                                 long bytesSize)
            throws IOException
    {
//        return this.of(RpmArtifactCoordinates.of(id, version)).buildPublishJson(bytesSize);
        return null;
    }

}
