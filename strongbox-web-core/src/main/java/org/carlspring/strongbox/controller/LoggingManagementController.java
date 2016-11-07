package org.carlspring.strongbox.controller;

import org.carlspring.logging.exceptions.AppenderNotFoundException;
import org.carlspring.logging.exceptions.LoggerNotFoundException;
import org.carlspring.logging.exceptions.LoggingConfigurationException;
import org.carlspring.logging.services.LoggingManagementService;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

import io.swagger.annotations.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * This controller provides a simple wrapper over REST API for the LoggingManagementService.
 *
 * @author Martin Todorov
 */
@Controller
@Api(value = "/logging")
@RequestMapping("/logging")
public class LoggingManagementController
        extends BaseController
{

    @Inject
    LoggingManagementService loggingManagementService;

    @RequestMapping(value = "/logger", method = RequestMethod.PUT, produces = TEXT_PLAIN_VALUE)
    public ResponseEntity addLogger(@RequestParam("logger") String loggerPackage,
                                    @RequestParam("level") String level,
                                    @RequestParam("appenderName") String appenderName)
    {
        try
        {
            loggingManagementService.addLogger(loggerPackage, level, appenderName);

            return ResponseEntity.ok("The logger was added successfully.");
        }
        catch (LoggingConfigurationException | AppenderNotFoundException e)
        {
            logger.trace(e.getMessage(), e);

            return ResponseEntity.status(BAD_REQUEST).body("Failed to add logger!");
        }
    }

    @RequestMapping(value = "/logger", method = RequestMethod.POST, produces = TEXT_PLAIN_VALUE)
    public ResponseEntity updateLogger(@RequestParam("logger") String loggerPackage,
                                       @RequestParam("level") String level)
    {
        try
        {
            loggingManagementService.updateLogger(loggerPackage, level);

            return ResponseEntity.ok("The logger was updated successfully.");
        }
        catch (LoggingConfigurationException e)
        {
            logger.trace(e.getMessage(), e);

            return ResponseEntity.status(BAD_REQUEST).body("Failed to update logger!");
        }
        catch (LoggerNotFoundException e)
        {
            return ResponseEntity.status(NOT_FOUND).body("Logger '" + loggerPackage + "' not found!");
        }
    }

    @RequestMapping(value = "/logger", method = RequestMethod.DELETE, produces = TEXT_PLAIN_VALUE)
    public ResponseEntity deleteLogger(@RequestParam("logger") String loggerPackage)
            throws IOException
    {
        try
        {
            loggingManagementService.deleteLogger(loggerPackage);

            return ResponseEntity.ok("The logger was deleted successfully.");
        }
        catch (LoggingConfigurationException e)
        {
            logger.trace(e.getMessage(), e);

            return ResponseEntity.status(BAD_REQUEST).body("Failed to delete the logger!");
        }
        catch (LoggerNotFoundException e)
        {
            return ResponseEntity.status(NOT_FOUND).body("Logger '" + loggerPackage + "' not found!");
        }
    }

    @RequestMapping(value = "/log/**", method = RequestMethod.GET, produces = TEXT_PLAIN_VALUE)
    public void downloadLog(HttpServletRequest request,
                            HttpServletResponse response)
            throws Exception
    {
        try
        {
            String path = convertRequestToPath("logging", request, "log");
            logger.debug("Received a request to retrieve log file " + path + ".");

            InputStream is = loggingManagementService.downloadLog(path);
            copyToResponse(is, response);
            logger.debug("Received a request to retrieve log file " + path + ".");
            response.setStatus(OK.value());

        }
        catch (LoggingConfigurationException e)
        {
            logger.trace(e.getMessage(), e);

            response.setStatus(BAD_REQUEST.value());
        }
    }

    @RequestMapping(value = "/logback", method = RequestMethod.GET, produces = APPLICATION_XML_VALUE)
    public void downloadLogbackConfiguration(HttpServletResponse response)
            throws Exception
    {
        try
        {
            InputStream is = loggingManagementService.downloadLogbackConfiguration();
            copyToResponse(is, response);
            response.setStatus(OK.value());
        }
        catch (LoggingConfigurationException e)
        {
            logger.trace(e.getMessage(), e);

            response.setStatus(BAD_REQUEST.value());
        }
    }

    @RequestMapping(value = "/logback",
                    method = RequestMethod.POST,
                    consumes = APPLICATION_XML_VALUE,
                    produces = TEXT_PLAIN_VALUE)
    public ResponseEntity uploadLogbackConfiguration(HttpServletRequest request)
    {
        try
        {
            loggingManagementService.uploadLogbackConfiguration(request.getInputStream());

            return ResponseEntity.ok("Logback configuration uploaded successfully.");
        }
        catch (IOException | LoggingConfigurationException e)
        {
            logger.trace(e.getMessage(), e);

            return ResponseEntity.status(BAD_REQUEST).body("Failed to resolve the logging configuration!");
        }
    }

}
