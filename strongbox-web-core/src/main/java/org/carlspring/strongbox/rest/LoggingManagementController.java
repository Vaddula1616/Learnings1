package org.carlspring.strongbox.rest;

import org.carlspring.logging.rest.AbstractLoggingManagementRestlet;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author carlspring
 */
@Controller
@RequestMapping("/logging")
public class LoggingManagementController
        extends AbstractLoggingManagementRestlet
{


    @ApiOperation(value = "Used to add a logger.", position = 0)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The logger was added successfully."),
                            @ApiResponse(code = 500, message = "Failed to add logger!") })
    @Override
    @PreAuthorize("hasAuthority('CONFIGURATION_ADD_LOGGER')")
    public Response addLogger(@RequestParam(value = "The package to log", required = true)
                                      String loggerPackage,
                              @RequestParam(value = "The logging level", required = true)
                                      String level,
                              @RequestParam(value = "The name of the appender", required = true)
                                      String appenderName)
    {
        return super.addLogger(loggerPackage, level, appenderName);
    }

    @ApiOperation(value = "Used to update a logger.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The logger was updated successfully."),
                            @ApiResponse(code = 400, message = "Failed to update logger!"),
                            @ApiResponse(code = 404, message = "Logger '${loggerPackage}' not found.") })
    @Override
    @PreAuthorize("hasAuthority('CONFIGURATION_UPDATE_LOGGER')")
    public Response updateLogger(@RequestParam(value = "The package to log", required = true)
                                         String loggerPackage,
                                 @RequestParam(value = "The logging level", required = true)
                                         String level)
    {
        return super.updateLogger(loggerPackage, level);
    }

    @ApiOperation(value = "Used to delete a logger.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The logger was deleted successfully."),
                            @ApiResponse(code = 400, message = "Failed to delete the logger!"),
                            @ApiResponse(code = 404, message = "Logger '${loggerPackage}' not found.") })
    @Override
    @PreAuthorize("hasAuthority('CONFIGURATION_DELETE_LOGGER')")
    public Response deleteLogger(@RequestParam(value = "The logger to delete", required = true)
                                         String loggerPackage)
            throws IOException
    {
        return super.deleteLogger(loggerPackage);
    }

    @ApiOperation(value = "Used to download a log file.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "", response = String.class),
                            @ApiResponse(code = 400, message = "Failed to resolve the log!") })
    @Override
    @PreAuthorize("hasAuthority('CONFIGURATION_RETRIEVE_LOG')")
    public Response downloadLog(@RequestParam(value = "The relative path to the log file", required = true)
                                        String path)
    {
        return super.downloadLog(path);
    }

    @ApiOperation(value = "Used to download the Logback configuration file.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = ""),
                            @ApiResponse(code = 400, message = "Failed to resolve the logging configuration!") })
    @Override
    @PreAuthorize("hasAuthority('CONFIGURATION_RETRIEVE_LOGBACK_CFG')")
    public Response downloadLogbackConfiguration()
    {
        return super.downloadLogbackConfiguration();
    }

    @ApiOperation(value = "Used to upload and re-load a Logback configuration file.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Logback configuration uploaded successfully."),
                            @ApiResponse(code = 400, message = "Failed to resolve the logging configuration!") })
    @Override
    @PreAuthorize("hasAuthority('CONFIGURATION_UPLOAD_LOGBACK_CFG')")
    public Response uploadLogbackConfiguration(
                                                      @RequestParam(
                                                              value = "The input stream of the the Logback configuration file to load",
                                                              required = true)
                                                              InputStream is)
    {
        return super.uploadLogbackConfiguration(is);
    }
}
