package org.carlspring.strongbox.artifact.generator;

/**
 * Created by yury on 9/28/16.
 */
@Deprecated
public class SpringArtifactDeployer
        extends MavenArtifactDeployer
{

//    private static final Logger logger = LoggerFactory.getLogger(MavenArtifactDeployer.class);
//
//    private String username;
//
//    private String password;
//
//    private ArtifactSpringClient client;
//
//    private MetadataMerger metadataMerger;
//
//
//    public SpringArtifactDeployer()
//    {
//    }
//
//    public SpringArtifactDeployer(String basedir)
//    {
//        super(basedir);
//    }
//
//    public SpringArtifactDeployer(File basedir)
//    {
//        super(basedir);
//    }
//
//    public void initializeClient()
//    {
//        client = ArtifactSpringClient.getTestInstance();
//    }
//
//    public void generateAndDeployArtifact(Artifact artifact,
//                                          String storageId,
//                                          String repositoryId)
//            throws NoSuchAlgorithmException,
//                   XmlPullParserException,
//                   IOException,
//                   ArtifactOperationException
//    {
//        generateAndDeployArtifact(artifact, null, storageId, repositoryId, "jar");
//    }
//
//    public void generateAndDeployArtifact(Artifact artifact,
//                                          String[] classifiers,
//                                          String storageId,
//                                          String repositoryId,
//                                          String packaging)
//            throws NoSuchAlgorithmException,
//                   XmlPullParserException,
//                   IOException,
//                   ArtifactOperationException
//    {
//        if (client == null)
//        {
//            initializeClient();
//        }
//
//        generatePom(artifact, packaging);
//        createArchive(artifact);
//
//        deploy(artifact, storageId, repositoryId);
//        deployPOM(ArtifactUtils.getPOMArtifact(artifact), storageId, repositoryId);
//
//        if (classifiers != null)
//        {
//            for (String classifier : classifiers)
//            {
//                // We're assuming the type of the classifier is the same as the one of the main artifact
//                Artifact artifactWithClassifier = ArtifactUtils.getArtifactFromGAVTC(artifact.getGroupId() + ":" +
//                                                                                     artifact.getArtifactId() + ":" +
//                                                                                     artifact.getVersion() + ":" +
//                                                                                     artifact.getType() + ":" +
//                                                                                     classifier);
//                generate(artifactWithClassifier);
//
//                deploy(artifactWithClassifier, storageId, repositoryId);
//            }
//        }
//        try
//        {
//            mergeMetada(artifact, storageId, repositoryId);
//        }
//        catch (ArtifactTransportException e)
//        {
//            // TODO SB-230: What should we do if we get ArtifactTransportException,
//            // IOException or XmlPullParserException
//            logger.error(e.getMessage(), e);
//        }
//    }
//
//
//    public void mergeMetada(Artifact artifact,
//                            String storageId,
//                            String repositoryId)
//            throws ArtifactTransportException,
//                   IOException,
//                   XmlPullParserException,
//                   NoSuchAlgorithmException,
//                   ArtifactOperationException
//    {
//        if (metadataMerger == null)
//        {
//            metadataMerger = new MetadataMerger();
//        }
//
//        Metadata metadata;
//        if (ArtifactUtils.isSnapshot(artifact.getVersion()))
//        {
//            String path = ArtifactUtils.getVersionLevelMetadataPath(artifact);
//            metadata = metadataMerger.updateMetadataAtVersionLevel(artifact,
//                                                                   retrieveMetadata("storages/" + storageId + "/" +
//                                                                                    repositoryId,
//                                                                                    ArtifactUtils.getVersionLevelMetadataPath(
//                                                                                            artifact)));
//
//            createMetadata(metadata, path);
//            deployMetadata(metadata, path, storageId, repositoryId);
//        }
//
//        String path = ArtifactUtils.getArtifactLevelMetadataPath(artifact);
//        metadata = metadataMerger.updateMetadataAtArtifactLevel(artifact,
//                                                                retrieveMetadata("storages/" + storageId + "/" +
//                                                                                 repositoryId,
//                                                                                 ArtifactUtils.getArtifactLevelMetadataPath(
//                                                                                         artifact)));
//
//        createMetadata(metadata, path);
//        deployMetadata(metadata, path, storageId, repositoryId);
//
//        if (artifact instanceof PluginArtifact)
//        {
//            path = ArtifactUtils.getGroupLevelMetadataPath(artifact);
//            metadata = metadataMerger.updateMetadataAtGroupLevel((PluginArtifact) artifact,
//                                                                 retrieveMetadata("storages/" + storageId + "/" +
//                                                                                  repositoryId,
//                                                                                  ArtifactUtils.getGroupLevelMetadataPath(
//                                                                                          artifact)));
//            createMetadata(metadata, path);
//            deployMetadata(metadata, path, storageId, repositoryId);
//        }
//    }
//
//
//    private void deployMetadata(Metadata metadata,
//                                String metadataPath,
//                                String storageId,
//                                String repositoryId)
//            throws IOException,
//                   NoSuchAlgorithmException,
//                   ArtifactOperationException
//    {
//        File metadataFile = new File(getBasedir(), metadataPath);
//
//        InputStream is = new FileInputStream(metadataFile);
//        MultipleDigestInputStream mdis = new MultipleDigestInputStream(is);
//
//        client.addMetadata(metadata, metadataPath, storageId, repositoryId, is);
//        deployChecksum(mdis,
//                       storageId,
//                       repositoryId,
//                       metadataPath.substring(0, metadataPath.lastIndexOf('/') + 1), "maven-metadata.xml");
//    }
//
//    private void deployChecksum(MultipleDigestInputStream mdis,
//                                String storageId,
//                                String repositoryId,
//                                String path,
//                                String metadataFileName)
//            throws ArtifactOperationException,
//                   IOException
//    {
//        mdis.getMessageDigestAsHexadecimalString(EncryptionAlgorithmsEnum.MD5.getAlgorithm());
//        mdis.getMessageDigestAsHexadecimalString(EncryptionAlgorithmsEnum.SHA1.getAlgorithm());
//
//        for (Map.Entry entry : mdis.getHexDigests().entrySet())
//        {
//            final String algorithm = (String) entry.getKey();
//            final String checksum = (String) entry.getValue();
//
//            ByteArrayInputStream bais = new ByteArrayInputStream(checksum.getBytes());
//
//            final String extensionForAlgorithm = EncryptionAlgorithmsEnum.fromAlgorithm(algorithm).getExtension();
//
//            String artifactToPath = path + metadataFileName + extensionForAlgorithm;
//            String url = client.getContextBaseUrl() + "/storages/" + storageId + "/" + repositoryId;
//            String artifactFileName = metadataFileName + extensionForAlgorithm;
//
//            client.deployFile(bais, url, artifactToPath, artifactFileName);
//        }
//    }
//
//    private Metadata retrieveMetadata(String url,
//                                      String path)
//            throws ArtifactTransportException,
//                   IOException,
//                   XmlPullParserException
//    {
//        if (client.pathExists(path, url))
//        {
//            InputStream is = client.getResource(path, url);
//            MetadataXpp3Reader reader = new MetadataXpp3Reader();
//
//            return reader.read(is);
//        }
//
//        return null;
//    }
//
//    public void deploy(Artifact artifact,
//                       String storageId,
//                       String repositoryId)
//            throws ArtifactOperationException, IOException, NoSuchAlgorithmException, XmlPullParserException
//    {
//        File artifactFile = new File(getBasedir(), ArtifactUtils.convertArtifactToPath(artifact));
//        ArtifactInputStream ais = new ArtifactInputStream(new MavenArtifactCoordinates(artifact), new FileInputStream(artifactFile));
//
//        client.addArtifact(artifact, storageId, repositoryId, ais);
//
//        deployChecksum(ais, storageId, repositoryId, artifact);
//    }
//
//    /*
//    private void deployChecksum(MultipleDigestInputStream mdis,
//                                String storageId,
//                                String repositoryId,
//                                String path,
//                                String metadataFileName)
//            throws ArtifactOperationException, IOException
//    {
//        mdis.getMessageDigestAsHexadecimalString(EncryptionAlgorithmsEnum.MD5.getAlgorithm());
//        mdis.getMessageDigestAsHexadecimalString(EncryptionAlgorithmsEnum.SHA1.getAlgorithm());
//
//        for (Map.Entry entry : mdis.getHexDigests().entrySet())
//        {
//            final String algorithm = (String) entry.getKey();
//            final String checksum = (String) entry.getValue();
//
//            ByteArrayInputStream bais = new ByteArrayInputStream(checksum.getBytes());
//
//            final String extensionForAlgorithm = EncryptionAlgorithmsEnum.fromAlgorithm(algorithm).getExtension();
//
//            String artifactToPath = path + metadataFileName +extensionForAlgorithm;
//            String url = client.getContextBaseUrl() + "/storages/" + storageId + "/" + repositoryId + "/"
//                    + artifactToPath;
//            String artifactFileName = metadataFileName + extensionForAlgorithm;
//
//            client.deployFile(bais, url, artifactToPath, artifactFileName);
//        }
//    }
//    */
//
//    private void deployPOM(Artifact artifact,
//                           String storageId,
//                           String repositoryId)
//            throws NoSuchAlgorithmException,
//                   IOException,
//                   ArtifactOperationException
//    {
//        File pomFile = new File(getBasedir(), ArtifactUtils.convertArtifactToPath(artifact));
//
//        InputStream is = new FileInputStream(pomFile);
//        ArtifactInputStream ais = new ArtifactInputStream(new MavenArtifactCoordinates(artifact), is);
//
//        client.addArtifact(artifact, storageId, repositoryId, ais);
//
//        deployChecksum(ais, storageId, repositoryId, );
//    }
//
//    public String getUsername()
//    {
//        return username;
//    }
//
//    public void setUsername(String username)
//    {
//        this.username = username;
//    }
//
//    public String getPassword()
//    {
//        return password;
//    }
//
//    public void setPassword(String password)
//    {
//        this.password = password;
//    }
//
//    public ArtifactSpringClient getClient()
//    {
//        return client;
//    }
//
//    public void setClient(ArtifactSpringClient client)
//    {
//        this.client = client;
//    }

}
