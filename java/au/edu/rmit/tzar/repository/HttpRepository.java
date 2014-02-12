package au.edu.rmit.tzar.repository;

import au.edu.rmit.tzar.api.TzarException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/**
 * Represents a resource stored on an http server. As http is not
 * a versioned protocol, revision parameters are ignored.
 */
public class HttpRepository extends UrlRepository {
  private static final Logger LOG = Logger.getLogger(HttpRepository.class.getName());

  // some websites give different responses, depending on the user agent. We gamble here
  // that we'll get the most sane response for something that doesn't look like a browser.
  // This won't work
  private static final String USER_AGENT = "Wget/1.12";

  @VisibleForTesting
  final CloseableHttpClient client;

  public HttpRepository(File baseModelsPath, URI sourceUri) {
    super(baseModelsPath, sourceUri);
    client = CachingHttpClientBuilder.create()
        .setCacheConfig(CacheConfig.DEFAULT)
        .setUserAgent(USER_AGENT)
        .build();
    // the line below should do the same as the line above, but there seems to be a bug which
    // causes a null pointer exception.
    // client = CachingHttpClients.createMemoryBound();
  }

  @Override
  public File retrieveModel(String revision, String name) throws TzarException {
    LOG.info(String.format("Retrieving model from %s",  sourceUri));
    return retrieveFile(createModelPath(name));
  }

  @Override
  public File retrieveProjectParams(String projectParamFilename, String revision) throws TzarException {
    File tempDir = Files.createTempDir();

    URI uri;
    try {
      uri = new URIBuilder(sourceUri).setPath(sourceUri.getPath() + "/" + projectParamFilename).build();
    } catch (URISyntaxException e) {
      throw new TzarException(e);
    }
    LOG.info(String.format("Retrieving project.yaml from: %s to local path: %s", uri, tempDir));
    return retrieveFile(createModelPath("project_params"));
  }

  @Override
  public String getHeadRevision() throws TzarException {
    return "";
  }

  File retrieveFile(File outputFile) throws TzarException {
    try {
      CloseableHttpResponse response = client.execute(new HttpGet(sourceUri));
      boolean exceptionOccurred = true;
      try {
        HttpEntity entity = response.getEntity();
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        entity.writeTo(fileOutputStream);
        exceptionOccurred = false;
        return outputFile;
      } finally {
        Closeables.close(response, exceptionOccurred);
      }
    } catch (IOException e) {
      throw new TzarException(e);
    }
  }
}