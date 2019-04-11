package org.carlspring.strongbox.dependency.snippet;

import org.carlspring.strongbox.artifact.coordinates.ArtifactCoordinates;
import org.carlspring.strongbox.artifact.coordinates.PypiWheelArtifactCoordinates;
import org.carlspring.strongbox.providers.layout.AbstractLayoutProvider;
import org.carlspring.strongbox.providers.layout.PypiLayoutProvider;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PypiWheelDependencyFormatter
  implements DependencySynonymFormatter
{
  private static final Logger logger = LoggerFactory.getLogger(AbstractLayoutProvider.class);

  @Inject
  private CompatibleDependencyFormatRegistry compatibleDependencyFormatRegistry;

  @PostConstruct
  @Override
  public void register()
  {
     compatibleDependencyFormatRegistry.addProviderImplementation(getLayout(), getFormatAlias(), this);
     logger.debug("Initialized the Pypi dependency formatter.");
  }

  @Override
  public String getLayout()
  {
    return PypiLayoutProvider.ALIAS;
  }

  @Override
  public String getFormatAlias()
  {
    return PypiLayoutProvider.ALIAS;
  }

  /**
  * This method takes in a set of Pypi Artifact Coordinates and returns the properly formatted dependency snippet
  * ex: Django>=1.8.4
  **/
  @Override
  public String getDependencySnippet(ArtifactCoordinates input_coordinates)
  {
    PypiWheelArtifactCoordinates coordinates = (PypiWheelArtifactCoordinates) input_coordinates;
    String version = coordinates.getVersion();
    String sb;
    
    if (version.charAt(version.length() - 1) == '*')
    {
      sb = coordinates.getID() + "==" + version;
    }
    else
    {
      sb = coordinates.getID() + ">=" + version;
    }

    return sb;
  }
}
