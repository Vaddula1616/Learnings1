package org.carlspring.strongbox.controller;

import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.configuration.ConfigurationManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Provides common subroutines that will be useful for any backend controller.
 *
 * @author Alex Oreshkevich
 */
public abstract class BaseController
{

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected ConfigurationManager configurationManager;


    protected ResponseEntity toError(String message)
    {
        return toError(new RuntimeException(message));
    }

    protected ResponseEntity toError(Throwable cause)
    {
        logger.error(cause.getMessage(), cause);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(cause.getMessage());
    }

    public String convertRequestToPath(String rootMapping,
                                       HttpServletRequest request,
                                       String... pathVariables)
    {
        int pathVarsLength = 0;
        int pathVarsCount = 0;
        for (String pathVariable : pathVariables)
        {
            if (pathVariable != null)
            {
                pathVarsLength += pathVariable.length();
                pathVarsCount++;
            }
        }

        int totalPrefixLength = rootMapping.length() + pathVarsLength + pathVarsCount + 1;
        int requestUriLength = request.getRequestURI().length();

        // process "/" and "" paths
        if (totalPrefixLength == requestUriLength || totalPrefixLength == requestUriLength + 1)
        {
            return "/";
        }

        if (requestUriLength > totalPrefixLength)
        {
            return request.getRequestURI().substring(totalPrefixLength);
        }
        else
        {
            logger.warn("Unable to calculate path for request uri " + request.getRequestURI());
            return null;
        }
    }

    protected Configuration getConfiguration()
    {
        return configurationManager.getConfiguration();
    }

    protected void copyToResponse(InputStream inputStream,
                                  HttpServletResponse response)
            throws Exception
    {
        try
        {
            long totalBytes = ByteStreams.copy(new BufferedInputStream(inputStream), response.getOutputStream());
            response.setHeader("Content-Length", totalBytes + "");
            response.flushBuffer();
            inputStream.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable copy to response", e);
        }
    }

}
