package org.carlspring.strongbox.aql;

import org.carlspring.strongbox.aql.grammar.AqlQueryParser;
import org.carlspring.strongbox.data.criteria.OQueryTemplate;
import org.carlspring.strongbox.data.criteria.Predicate;
import org.carlspring.strongbox.data.criteria.QueryParserException;
import org.carlspring.strongbox.data.criteria.Selector;
import org.carlspring.strongbox.domain.ArtifactEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.javatuples.Pair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.*;

public class AqlParserTest
{

    private static final Logger logger = LoggerFactory.getLogger(AqlParserTest.class);


    @Test
    public void testLayoutSpecificKeywords()
    {
        String query = "storage:storage-common-proxies +repository:carlspring +groupId:'org.carlspring'" +
                       " +artifactId:'some strange group' asc: version";

        AqlQueryParser aqlParser = new AqlQueryParser(query);

        logger.debug(String.format("Query [%s] parse tree:\n[%s]", query, aqlParser));

        Selector<ArtifactEntry> selector = aqlParser.parseQuery();
        Predicate predicate = selector.getPredicate();

        assertNotNull(predicate);
        assertFalse(predicate.isEmpty());
        assertFalse(aqlParser.hasErrors());

        query = "storage:storage-common-proxies +repository:carlspring +invalidId:'org.carlspring'" +
                " +artifactId:'test-artifact' asc: unknownCoordinateId";

        aqlParser = new AqlQueryParser(query);

        logger.debug(String.format("Query [%s] parse tree:\n[%s]", query, aqlParser));

        Map<Pair<Integer, Integer>, String> errorMap = null;
        try
        {
            aqlParser.parseQuery();
        }
        catch (QueryParserException e)
        {
            errorMap = aqlParser.getErrors();
        }

        logger.debug(String.format("Query [%s] parse tree:\n[%s]", query, aqlParser));

        assertTrue(aqlParser.hasErrors());
        assertNotNull(errorMap);
        assertEquals(2, errorMap.size());

        List<Pair<Integer, Integer>> errorPositionList = new ArrayList<>(errorMap.keySet());

        assertEquals(Pair.with(1, 55), errorPositionList.get(0));
        assertEquals(Pair.with(1, 115), errorPositionList.get(1));
    }

    @Test
    public void testValidDoubleQuotedQuerySpecificChars()
    {
        String query = "repository: \"releases#1\"";

        AqlQueryParser aqlParser = new AqlQueryParser(query);

        aqlParser.parseQuery();

        logger.debug(String.format("Query [%s] parse tree:\n[%s]", query, aqlParser));

        assertFalse(aqlParser.hasErrors());
    }

    @Test
    public void testValidSingleQuotedQuerySpecificChars()
    {
        String query = "repository: 'releases#1'";

        AqlQueryParser aqlParser = new AqlQueryParser(query);

        aqlParser.parseQuery();

        logger.debug(String.format("Query [%s] parse tree:\n[%s]", query, aqlParser));

        assertFalse(aqlParser.hasErrors());
    }

    @Test
    public void testInvalidUnquotedStringQuery()
    {
        String query = "repository: releases#1";

        AqlQueryParser aqlParser = new AqlQueryParser(query);
        Map<Pair<Integer, Integer>, String> errorMap = null;
        try
        {
            aqlParser.parseQuery();
        }
        catch (QueryParserException e)
        {
            errorMap = aqlParser.getErrors();
        }

        logger.debug(String.format("Query [%s] parse tree:\n[%s]", query, aqlParser));

        assertTrue(aqlParser.hasErrors());
        assertNotNull(errorMap);

        List<Pair<Integer, Integer>> errorPositionList = new ArrayList<>(errorMap.keySet());

        assertEquals(Pair.with(1, 20), errorPositionList.get(0));
        assertEquals(Pair.with(1, 21), errorPositionList.get(1));
    }

    @Test
    public void testValidUnquotedStringQuery()
    {
        String query = "repository: releases_1";

        AqlQueryParser aqlParser = new AqlQueryParser(query);

        aqlParser.parseQuery();

        logger.debug(String.format("Query [%s] parse tree:\n[%s]", query, aqlParser));

        assertFalse(aqlParser.hasErrors());
    }

    @Test
    @Disabled
    public void testValidQueryWithUpperLowercaseCheck()
    {
        String query = "(storagE:storage-common-proxies) +Repository:carlspring Or +(groupId:'org.carlspring')" +
                       " AnD (!(artifactId:'some strange group') || !version:0.*) aSc: agE sKip: 12";

        AqlQueryParser aqlParser = new AqlQueryParser(query);

        logger.debug(String.format("Query [%s] parse tree:\n[%s]", query, aqlParser));

        Selector<ArtifactEntry> selector = aqlParser.parseQuery();
        Predicate predicate = selector.getPredicate();

        assertNotNull(predicate);
        assertFalse(predicate.isEmpty());
        assertFalse(aqlParser.hasErrors());

        OQueryTemplate<Object, ArtifactEntry> queryTemplate = new OQueryTemplate<>(null);

        String sqlQuery = queryTemplate.calculateQueryString(selector);

        logger.debug(String.format("Query [%s] parse result:\n[%s]", query, sqlQuery));

        assertEquals("SELECT * " +
                     "FROM " +
                     "ArtifactEntry " +
                     "WHERE " +
                     "artifactCoordinates IS NOT NULL  " +
                     "AND ((storageId = :storageId_0) " +
                     "AND repositoryId = :repositoryId_1 " +
                     "OR (artifactCoordinates.coordinates.groupId = :groupId_1) " +
                     "AND ( NOT ((artifactCoordinates.coordinates.artifactId = :artifactId_1)) OR " +
                     " NOT (artifactCoordinates.version LIKE :version_2))) " +
                     "ORDER BY lastUpdated ASC " +
                     "SKIP 12 " +
                     "LIMIT 25",
                     sqlQuery);

        Map<String, Object> parameterMap = queryTemplate.exposeParameterMap(predicate);

        logger.debug(String.format("Query [%s] parse parameters:\n[%s]", query, parameterMap));

        assertEquals(ImmutableMap.of("storageId_0",
                                     "storage-common-proxies",
                                     "repositoryId_1",
                                     "carlspring",
                                     "groupId_1",
                                     "org.carlspring",
                                     "version_2",
                                     "0.%",
                                     "artifactId_1",
                                     "some strange group"),
                     parameterMap);
    }

    @Test
    public void testInvalidQuery()
    {
        String query = "[storage:storage0] ++repository0:releases ||| & groupId:org.carlspring-version:1.2.3)";

        AqlQueryParser aqlParser = new AqlQueryParser(query);
        Map<Pair<Integer, Integer>, String> errorMap = null;
        try
        {
            aqlParser.parseQuery();
        }
        catch (QueryParserException e)
        {
            errorMap = aqlParser.getErrors();
        }

        logger.debug(String.format("Query [%s] parse tree:\n[%s]", query, aqlParser));

        assertTrue(aqlParser.hasErrors());
        assertNotNull(errorMap);
        assertEquals(4, errorMap.size());

        List<Pair<Integer, Integer>> errorPositionList = new ArrayList<>(errorMap.keySet());

        assertEquals(Pair.with(1, 0), errorPositionList.get(0));
        assertEquals(Pair.with(1, 17), errorPositionList.get(1));
        assertEquals(Pair.with(1, 20), errorPositionList.get(2));
        assertEquals(Pair.with(1, 78), errorPositionList.get(3));
    }

}
