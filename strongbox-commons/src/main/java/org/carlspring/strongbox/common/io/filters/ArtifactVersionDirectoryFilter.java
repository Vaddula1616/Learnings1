package org.carlspring.strongbox.common.io.filters;

import org.carlspring.commons.io.filters.DirectoryFilter;
import org.carlspring.maven.commons.io.filters.PomFilenameFilter;

import java.io.File;

/**
 * @author mtodorov
 */
public class ArtifactVersionDirectoryFilter
        extends DirectoryFilter
{

    private boolean excludeHiddenDirectories = true;


    public ArtifactVersionDirectoryFilter()
    {
    }

    public ArtifactVersionDirectoryFilter(boolean excludeHiddenDirectories)
    {
        this.excludeHiddenDirectories = excludeHiddenDirectories;
    }

    @Override
    public boolean accept(File file)
    {
        return super.accept(file) && containsPomFiles(file);
    }

    private boolean containsPomFiles(File file)
    {
        if (file.isDirectory())
        {
            File[] directories = file.listFiles(new PomFilenameFilter());

            return directories != null && directories.length > 0;
        }
        else
        {
            return false;
        }
    }

    public boolean excludeHiddenDirectories()
    {
        return excludeHiddenDirectories;
    }

    public void setExcludeHiddenDirectories(boolean excludeHiddenDirectories)
    {
        this.excludeHiddenDirectories = excludeHiddenDirectories;
    }

}
