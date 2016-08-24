package org.carlspring.strongbox.rest;

import org.carlspring.strongbox.services.ArtifactSearchService;
import org.carlspring.strongbox.storage.indexing.SearchRequest;
import org.carlspring.strongbox.storage.indexing.SearchResults;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/search")
public class SearchController
        extends BaseArtifactRestlet
{

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private ArtifactSearchService artifactSearchService;

    /**
     * Performs a search against the Lucene index of a specified repository,
     * or the Lucene indexes of all repositories.
     */
    @ApiOperation(value = "Used to search for artifacts.", response = SearchResults.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "") })
    @PreAuthorize("hasAuthority('SEARCH_ARTIFACTS')")
    @RequestMapping(method = RequestMethod.GET, produces = { MediaType.APPLICATION_XML_VALUE,
                                                             MediaType.APPLICATION_JSON_VALUE,
                                                             MediaType.TEXT_PLAIN_VALUE })
    public ResponseEntity search(@RequestParam(value = "The storageId", name = "storageId", required = true)
                                 final String storageId,
                                 @RequestParam(value = "The repositoryId", name = "repositoryId", required = true)
                                 final String repositoryId,
                                 @RequestParam(value = "The search query", name = "q", required = true)
                                 final String query,
                                 HttpHeaders headers,
                                 HttpServletRequest request)
            throws IOException, ParseException
    {
        if (request.getHeader("accept").equalsIgnoreCase("text/plain"))
        {
            final SearchResults artifacts = getSearchResults(storageId, repositoryId, query);

            return ResponseEntity.ok(artifacts.toString());
        }
        else
        {
            // Apparently, the JSON root tag's name is based on the name of the object
            // which the Jersey method returns, hence this is "artifacts".
            @SuppressWarnings("UnnecessaryLocalVariable")
            final SearchResults artifacts = getSearchResults(storageId, repositoryId, query);

            return ResponseEntity.ok(artifacts);
        }
    }

    private SearchResults getSearchResults(String storageId,
                                           String repositoryId,
                                           String query)
            throws IOException, ParseException
    {
        final SearchRequest searchRequest = new SearchRequest(storageId, repositoryId, query);

        return artifactSearchService.search(searchRequest);
    }

}
