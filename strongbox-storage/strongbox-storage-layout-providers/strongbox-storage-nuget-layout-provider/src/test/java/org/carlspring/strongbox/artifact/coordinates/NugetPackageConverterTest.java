package org.carlspring.strongbox.artifact.coordinates;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class NugetPackageConverterTest
{

    @Test
    public void testArtifactPathToCoordinatesConversion()
        throws Exception
    {
        String path = "Org.Carlspring.Strongbox.Examples.Nuget.Mono/1.0/Org.Carlspring.Strongbox.Examples.Nuget.Mono.1.0.nupkg";
        NugetArtifactCoordinates nac = new NugetArtifactCoordinates(path);
        assertEquals("Failed to convert path to artifact coordinates!", "Org.Carlspring.Strongbox.Examples.Nuget.Mono",
                     nac.getId());
        assertEquals("Failed to convert path to artifact coordinates!", "1.0", nac.getVersion());

        path = "Org.Carlspring.Strongbox.Examples.Nuget.Mono/1.0/Org.Carlspring.Strongbox.Examples.Nuget.Mono.nuspec";
        nac = new NugetArtifactCoordinates(path);
        assertEquals("Failed to convert path to artifact coordinates!", "Org.Carlspring.Strongbox.Examples.Nuget.Mono",
                     nac.getId());
        assertEquals("Failed to convert path to artifact coordinates!", "1.0", nac.getVersion());

        path = "Org.Carlspring.Strongbox.Examples.Nuget.Mono/1.0/Org.Carlspring.Strongbox.Examples.Nuget.Mono.1.0.nupkg.sha512";
        nac = new NugetArtifactCoordinates(path);
        assertEquals("Failed to convert path to artifact coordinates!", "Org.Carlspring.Strongbox.Examples.Nuget.Mono",
                     nac.getId());
        assertEquals("Failed to convert path to artifact coordinates!", "1.0", nac.getVersion());
    }

}
