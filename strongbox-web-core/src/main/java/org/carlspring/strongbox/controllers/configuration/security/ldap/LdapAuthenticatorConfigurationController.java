package org.carlspring.strongbox.controllers.configuration.security.ldap;

import org.carlspring.strongbox.authentication.support.AuthoritiesExternalToInternalMapper;
import org.carlspring.strongbox.controllers.BaseController;
import org.carlspring.strongbox.controllers.configuration.security.ldap.support.LdapGroupSearchResponseEntityBody;
import org.carlspring.strongbox.controllers.configuration.security.ldap.support.LdapMessages;
import org.carlspring.strongbox.controllers.configuration.security.ldap.support.LdapUserDnPatternsResponseEntityBody;
import org.carlspring.strongbox.controllers.configuration.security.ldap.support.LdapUserSearchResponseEntityBody;
import org.carlspring.strongbox.controllers.configuration.security.ldap.support.SpringSecurityLdapInternalsSupplier;
import org.carlspring.strongbox.controllers.configuration.security.ldap.support.SpringSecurityLdapConfigurationTester;
import org.carlspring.strongbox.controllers.configuration.security.ldap.support.SpringSecurityLdapInternalsUpdater;
import org.carlspring.strongbox.forms.configuration.security.ldap.LdapConfigurationForm;
import org.carlspring.strongbox.forms.configuration.security.ldap.LdapConfigurationTestForm;
import org.carlspring.strongbox.validation.RequestBodyValidationException;

import javax.inject.Inject;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Przemyslaw Fusik
 * @author Pablo Tirado
 */
@RestController
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping(value = "/api/configuration/ldap")
@Api(value = "/api/configuration/ldap")
public class LdapAuthenticatorConfigurationController
        extends BaseController
{

    private static final String FAILED_PUT_LDAP = "LDAP configuration cannot be updated because the submitted form contains errors!";

    private static final String FAILED_PUT_LDAP_TEST = "LDAP configuration cannot be tested because the submitted form contains errors!";

    private static final String ERROR_PUT_LDAP = "LDAP configuration update succeeded";

    private static final String SUCCESS_PUT_LDAP = "Failed to update LDAP configuration.";

    private static final String LDAP_TEST_PASSED = "LDAP configuration test passed";

    private static final String LDAP_TEST_FAILED = "LDAP configuration test failed";

    private static final String ERROR_PUT_LDAP_TEST = "Failed to test LDAP configuration.";

    private static final String SUCCESS_ADD_ROLE_MAPPING = "LDAP role mapping configuration update succeeded";

    @Inject
    private SpringSecurityLdapInternalsSupplier springSecurityLdapInternalsSupplier;

    @Inject
    private SpringSecurityLdapInternalsUpdater springSecurityLdapInternalsUpdater;

    @Inject
    private SpringSecurityLdapConfigurationTester springSecurityLdapInternalsTester;

    @ApiOperation(value = "Tests LDAP configuration settings")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "LDAP configuration test has passed.") })
    @PutMapping(value = "/test", produces = { MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public ResponseEntity testLdapConfiguration(@RequestBody @Validated LdapConfigurationTestForm form,
                                                BindingResult bindingResult,
                                                @RequestHeader(HttpHeaders.ACCEPT) String accept)
    {
        if (!springSecurityLdapInternalsSupplier.isLdapAuthenticationEnabled())
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, accept);
        }

        if (bindingResult.hasErrors())
        {
            throw new RequestBodyValidationException(FAILED_PUT_LDAP_TEST, bindingResult);
        }

        boolean result;
        try
        {
            result = springSecurityLdapInternalsTester.test(form);
        }
        catch (Exception e)
        {
            return getExceptionResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_PUT_LDAP_TEST, e, accept);
        }

        return getSuccessfulResponseEntity(result ? LDAP_TEST_PASSED : LDAP_TEST_FAILED, accept);
    }

    @ApiOperation(value = "Update the LDAP configuration settings")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "LDAP configuration updated successfully.") })
    @PutMapping(produces = { MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public ResponseEntity putLdapConfiguration(@RequestBody @Validated LdapConfigurationForm form,
                                               BindingResult bindingResult,
                                               @RequestHeader(HttpHeaders.ACCEPT) String accept)
    {
        if (!springSecurityLdapInternalsSupplier.isLdapAuthenticationEnabled())
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, accept);
        }

        if (bindingResult.hasErrors())
        {
            throw new RequestBodyValidationException(FAILED_PUT_LDAP, bindingResult);
        }

        try
        {
            springSecurityLdapInternalsUpdater.updateLdapConfigurationSettings(form);
        }
        catch (Exception e)
        {
            return getExceptionResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_PUT_LDAP, e, accept);
        }

        return getSuccessfulResponseEntity(SUCCESS_PUT_LDAP, accept);
    }

    @ApiOperation(value = "Returns LDAP configuration")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The LDAP configuration."),
                            @ApiResponse(code = 400, message = "LDAP is not enabled.") })
    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity getLdapConfiguration(@RequestHeader(HttpHeaders.ACCEPT) String acceptHeader)
    {
        if (!springSecurityLdapInternalsSupplier.isLdapAuthenticationEnabled())
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, acceptHeader);
        }

        ResponseEntity rolesMapping = getRolesMapping(acceptHeader);
        ResponseEntity groupSearchFilter = getGroupSearchFilter(acceptHeader);
        ResponseEntity userDnPatterns = getUserDnPatterns(acceptHeader);
        ResponseEntity userSearchFilter = getUserSearchFilter(acceptHeader);

        return ResponseEntity.ok(ImmutableSet.of(rolesMapping.getBody(),
                                                 userDnPatterns.getBody(),
                                                 ImmutableMap.of("groupSearchFilter", groupSearchFilter.getBody()),
                                                 ImmutableMap.of("userSearchFilter", userSearchFilter.getBody()))
        );
    }

    @ApiOperation(value = "Returns LDAP roles to strongbox internal roles mapping")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The mapping."),
                            @ApiResponse(code = 400, message = "LDAP is not enabled.") })
    @GetMapping(value = "/rolesMapping",
            produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity getRolesMapping(@RequestHeader(HttpHeaders.ACCEPT) String acceptHeader)
    {
        if (!springSecurityLdapInternalsSupplier.isLdapAuthenticationEnabled())
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, acceptHeader);
        }

        AuthoritiesExternalToInternalMapper body = springSecurityLdapInternalsSupplier.getAuthoritiesMapper();
        return ResponseEntity.ok(body);
    }

    @ApiOperation(value = "Adds LDAP role mapping if the mapping does not exist. It doesn't override existing value.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "LDAP role mapping addition succeeded"),
                            @ApiResponse(code = 400, message = "LDAP is not enabled or LDAP role mapping already exists for given LDAP role"),
                            @ApiResponse(code = 500, message = "Failed to add LDAP role mapping") })
    @PostMapping(value = "/rolesMapping/{externalRole}/{internalRole}",
            produces = { MediaType.TEXT_PLAIN_VALUE,
                         MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity addRoleMapping(@PathVariable String externalRole,
                                         @PathVariable String internalRole,
                                         @RequestHeader(HttpHeaders.ACCEPT) String acceptHeader)
    {
        if (!springSecurityLdapInternalsSupplier.isLdapAuthenticationEnabled())
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, acceptHeader);
        }
        try
        {
            String previousInternalRole = springSecurityLdapInternalsSupplier.getAuthoritiesMapper()
                                                                             .addRoleMapping(externalRole,
                                                                                             internalRole);
            if (previousInternalRole != null)
            {
                return getBadRequestResponseEntity(LdapMessages.ROLE_ALREADY_EXISTS, acceptHeader);
            }
        }
        catch (Exception e)
        {
            String message = "Failed to add LDAP role mapping.";
            return getExceptionResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message, e, acceptHeader);
        }

        return getSuccessfulResponseEntity(SUCCESS_ADD_ROLE_MAPPING, acceptHeader);
    }

    @ApiOperation(value = "Adds or updates LDAP role mapping")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "LDAP role mapping add or update succeeded"),
                            @ApiResponse(code = 400, message = "LDAP is not enabled"),
                            @ApiResponse(code = 500, message = "Failed to add or update LDAP role mapping") })
    @PutMapping(value = "/rolesMapping/{externalRole}/{internalRole}",
            produces = { MediaType.TEXT_PLAIN_VALUE,
                         MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity setRoleMapping(@PathVariable String externalRole,
                                         @PathVariable String internalRole,
                                         @RequestHeader(HttpHeaders.ACCEPT) String acceptHeader)
    {
        if (!springSecurityLdapInternalsSupplier.isLdapAuthenticationEnabled())
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, acceptHeader);
        }
        try
        {
            springSecurityLdapInternalsSupplier.getAuthoritiesMapper().putRoleMapping(externalRole, internalRole);
        }
        catch (Exception e)
        {
            String message = "Failed to add or update LDAP role mapping!";
            return getExceptionResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message, e, acceptHeader);
        }

        return getSuccessfulResponseEntity("LDAP role mapping add or update succeeded", acceptHeader);
    }


    @ApiOperation(value = "Deletes LDAP role mapping")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "LDAP role mapping deletion succeeded"),
                            @ApiResponse(code = 400, message = "LDAP is not enabled or externalRole does not exist in the LDAP roles mapping"),
                            @ApiResponse(code = 500, message = "Failed to delete the LDAP role mapping!") })
    @DeleteMapping(value = "/rolesMapping/{externalRole}",
            produces = { MediaType.TEXT_PLAIN_VALUE,
                         MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity deleteRoleMapping(@PathVariable String externalRole,
                                            @RequestHeader(HttpHeaders.ACCEPT) String acceptHeader)
    {
        if (!springSecurityLdapInternalsSupplier.isLdapAuthenticationEnabled())
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, acceptHeader);
        }
        try
        {
            String removedInternalRole = springSecurityLdapInternalsSupplier.getAuthoritiesMapper()
                                                                            .deleteRoleMapping(externalRole);
            if (removedInternalRole == null)
            {
                String message = String.format("%s role does not exist in the LDAP roles mapping", externalRole);
                return getBadRequestResponseEntity(message, acceptHeader);
            }
        }
        catch (Exception e)
        {
            String message = "Failed to delete the LDAP role mapping!";
            return getExceptionResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message, e, acceptHeader);
        }

        return getSuccessfulResponseEntity("LDAP role mapping deletion succeeded", acceptHeader);
    }

    @ApiOperation(value = "Returns user DN patterns. See http://docs.spring.io/spring-security/site/docs/current/reference/html/ldap.html#using-bind-authentication")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "User DN patterns."),
                            @ApiResponse(code = 204, message = "User DN patterns are empty."),
                            @ApiResponse(code = 400, message = "LDAP is not enabled.") })
    @GetMapping(value = "/userDnPatterns",
            produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity getUserDnPatterns(@RequestHeader(HttpHeaders.ACCEPT) String acceptHeader)
    {
        if (!springSecurityLdapInternalsSupplier.isLdapAuthenticationEnabled())
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, acceptHeader);
        }
        List<String> userDnPatterns = springSecurityLdapInternalsSupplier.getUserDnPatterns();
        if (userDnPatterns == null)
        {
            return ResponseEntity.noContent()
                                 .build();
        }

        LdapUserDnPatternsResponseEntityBody body = new LdapUserDnPatternsResponseEntityBody(userDnPatterns);
        return ResponseEntity.ok(body);
    }

    @ApiOperation(value = "Removes the provided user DN pattern from the userDnPatterns.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "User DN pattern removal from the userDnPatterns succeeded"),
                            @ApiResponse(code = 400, message = "LDAP is not enabled or pattern does not match any existing userDnPatterns"),
                            @ApiResponse(code = 500, message = "Failed to remove user DN pattern!") })
    @DeleteMapping(value = "/userDnPatterns/{pattern}",
            produces = { MediaType.TEXT_PLAIN_VALUE,
                         MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity deleteUserDnPattern(@PathVariable String pattern,
                                              @RequestHeader(HttpHeaders.ACCEPT) String acceptHeader)
    {
        if (!springSecurityLdapInternalsSupplier.isLdapAuthenticationEnabled())
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, acceptHeader);
        }
        List<String> userDnPatterns = springSecurityLdapInternalsSupplier.getUserDnPatterns();
        if (userDnPatterns == null)
        {
            return ResponseEntity.noContent()
                                 .build();
        }
        if (!userDnPatterns.remove(pattern))
        {
            return getBadRequestResponseEntity("Pattern does not match any existing userDnPatterns", acceptHeader);
        }
        try
        {
            springSecurityLdapInternalsUpdater.updateUserDnPatterns(userDnPatterns);
        }
        catch (Exception e)
        {
            String message = "Failed to remove user DN pattern!";
            return getExceptionResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message, e, acceptHeader);
        }

        String message = String.format("User DN pattern %s removed from the userDnPatterns", pattern);
        return getSuccessfulResponseEntity(message, acceptHeader);
    }

    @ApiOperation(value = "Adds the provided user DN pattern to the userDnPatterns.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "User DN pattern addition to the userDnPatterns succeeded"),
                            @ApiResponse(code = 400, message = "LDAP is not enabled or if userDnPatterns collection haven't changed"),
                            @ApiResponse(code = 500, message = "User DN pattern addition to the userDnPatterns failed with server error") })
    @PostMapping(value = "/userDnPatterns/{pattern}",
            produces = { MediaType.TEXT_PLAIN_VALUE,
                         MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity addUserDnPattern(@PathVariable String pattern,
                                           @RequestHeader(HttpHeaders.ACCEPT) String acceptHeader)
    {
        if (!springSecurityLdapInternalsSupplier.isLdapAuthenticationEnabled())
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, acceptHeader);
        }
        List<String> userDnPatterns = springSecurityLdapInternalsSupplier.getUserDnPatterns();
        if (userDnPatterns == null)
        {
            return ResponseEntity.noContent()
                                 .build();
        }
        if (!userDnPatterns.add(pattern))
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, acceptHeader);
        }
        try
        {
            springSecurityLdapInternalsUpdater.updateUserDnPatterns(userDnPatterns);
        }
        catch (Exception e)
        {
            String message = "User DN pattern addition failed. Check server logs for more information.";
            return getExceptionResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message, e, acceptHeader);
        }

        String message = String.format("User DN pattern %s added to the userDnPatterns", pattern);
        return getSuccessfulResponseEntity(message, acceptHeader);
    }

    @ApiOperation(value = "Returns user search filter. See http://docs.spring.io/spring-security/site/docs/current/reference/html/ldap.html#using-bind-authentication")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "User search filter."),
                            @ApiResponse(code = 204, message = "User search filter was not provided."),
                            @ApiResponse(code = 400, message = "LDAP is not enabled or userSearchFilter is not supported via this method.") })
    @GetMapping(value = "/userSearchFilter",
            produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity getUserSearchFilter(@RequestHeader(HttpHeaders.ACCEPT) String acceptHeader)
    {
        if (!springSecurityLdapInternalsSupplier.isLdapAuthenticationEnabled())
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, acceptHeader);
        }
        LdapUserSearch userSearch = springSecurityLdapInternalsSupplier.getUserSearch();
        if (userSearch == null)
        {
            return ResponseEntity.noContent()
                                 .build();
        }
        if (!(userSearch instanceof FilterBasedLdapUserSearch))
        {
            String message = String.format(
                    "Unable to display userSearchFilter configuration. %s is not supported via this method.",
                    userSearch.getClass());
            return getBadRequestResponseEntity(message, acceptHeader);
        }

        LdapUserSearchResponseEntityBody body = springSecurityLdapInternalsSupplier.getUserSearchXmlHolder(
                (FilterBasedLdapUserSearch) userSearch);
        return ResponseEntity.ok(body);
    }

    @ApiOperation(value = "Updates LDAP user search filter. See http://docs.spring.io/spring-security/site/docs/current/reference/html/ldap.html#using-bind-authentication")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "User search filter updated."),
                            @ApiResponse(code = 204, message = "AbstractLdapAuthenticator was not provided."),
                            @ApiResponse(code = 400, message = "LDAP is not enabled.") })
    @PutMapping(value = "/userSearchFilter/{searchBase}/{searchFilter}",
            produces = { MediaType.TEXT_PLAIN_VALUE,
                         MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity updateUserSearchFilter(@PathVariable String searchBase,
                                                 @PathVariable String searchFilter,
                                                 @RequestHeader(HttpHeaders.ACCEPT) String acceptHeader)
    {
        if (!springSecurityLdapInternalsSupplier.isLdapAuthenticationEnabled())
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, acceptHeader);
        }
        AbstractLdapAuthenticator abstractLdapAuthenticator = springSecurityLdapInternalsSupplier.getAuthenticator();
        if (abstractLdapAuthenticator == null)
        {
            return ResponseEntity.noContent()
                                 .build();
        }
        springSecurityLdapInternalsUpdater.updateUserSearchFilter(abstractLdapAuthenticator, searchBase, searchFilter);

        return getSuccessfulResponseEntity("User search filter updated.", acceptHeader);
    }

    @ApiOperation(value = "Returns group search filter. See http://docs.spring.io/spring-security/site/docs/current/reference/html/ldap.html#loading-authorities")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Group search filter."),
                            @ApiResponse(code = 204, message = "Group search filter was not provided."),
                            @ApiResponse(code = 400, message = "LDAP is not enabled or groupSearchFilter is not supported via this method.") })
    @GetMapping(value = "/groupSearchFilter",
            produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity getGroupSearchFilter(@RequestHeader(HttpHeaders.ACCEPT) String acceptHeader)
    {
        if (!springSecurityLdapInternalsSupplier.isLdapAuthenticationEnabled())
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, acceptHeader);
        }
        LdapAuthoritiesPopulator populator = springSecurityLdapInternalsSupplier.getAuthoritiesPopulator();
        if (populator == null)
        {
            return ResponseEntity.noContent()
                                 .build();
        }
        if (!(populator instanceof DefaultLdapAuthoritiesPopulator))
        {
            String message = String.format(
                    "Unable to display groupSearchFilter configuration. %s is not supported via this method.",
                    populator.getClass());
            return getBadRequestResponseEntity(message, acceptHeader);
        }

        LdapGroupSearchResponseEntityBody body = springSecurityLdapInternalsSupplier.ldapGroupSearchHolder(
                (DefaultLdapAuthoritiesPopulator) populator);
        return ResponseEntity.ok(body);
    }

    @ApiOperation(value = "Updates LDAP group search filter. See http://docs.spring.io/spring-security/site/docs/current/reference/html/ldap.html#loading-authorities")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Group search filter updated."),
                            @ApiResponse(code = 204, message = "LdapAuthoritiesPopulator was not provided."),
                            @ApiResponse(code = 400, message = "LDAP is not enabled or ldapAuthoritiesPopulator class is not supported via this method.") })
    @PutMapping(value = "/groupSearchFilter/{searchBase}/{searchFilter}",
            produces = { MediaType.TEXT_PLAIN_VALUE,
                         MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity updateGroupSearchFilter(@PathVariable String searchBase,
                                                  @PathVariable String searchFilter,
                                                  @RequestHeader(HttpHeaders.ACCEPT) String acceptHeader)
    {
        if (!springSecurityLdapInternalsSupplier.isLdapAuthenticationEnabled())
        {
            return getBadRequestResponseEntity(LdapMessages.NOT_CONFIGURED, acceptHeader);
        }
        LdapAuthoritiesPopulator ldapAuthoritiesPopulator = springSecurityLdapInternalsSupplier.getAuthoritiesPopulator();
        if (ldapAuthoritiesPopulator == null)
        {
            return ResponseEntity.noContent()
                                 .build();
        }
        if (!(ldapAuthoritiesPopulator instanceof DefaultLdapAuthoritiesPopulator))
        {
            return getBadRequestResponseEntity(
                    "Configured ldapAuthoritiesPopulator is not supported. LDAP has to be configured with DefaultLdapAuthoritiesPopulator.",
                    acceptHeader);
        }
        springSecurityLdapInternalsUpdater.updateGroupSearchFilter(
                (DefaultLdapAuthoritiesPopulator) ldapAuthoritiesPopulator, searchBase, searchFilter);

        return getSuccessfulResponseEntity("Group search filter updated.", acceptHeader);
    }

}
