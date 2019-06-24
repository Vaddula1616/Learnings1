package org.carlspring.strongbox.util;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.carlspring.strongbox.domain.PypiPackageInfo;

import org.carlspring.strongbox.util.annotations.PypiMetadataKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;

@Component
public class PypiMetadataParser
{

    private static final Logger logger = LoggerFactory.getLogger(PypiMetadataParser.class);

    public PypiPackageInfo parseMetadataFile(InputStream is)
            throws IllegalAccessException, IOException
    {
        Map<String, String> keyValueMap = generateKeyValueMap(is);
        PypiPackageInfo dao = new PypiPackageInfo();
        return populateAnnotatedFields(dao, keyValueMap);
    }

    private PypiPackageInfo populateAnnotatedFields(PypiPackageInfo object, Map<String, String> keyValueMap)
            throws IllegalAccessException
    {
        Class<?> clazz = object.getClass();
        for (Field field: clazz.getDeclaredFields())
        {
            field.setAccessible(true);
            try
            {
                if (field.isAnnotationPresent(PypiMetadataKey.class))
                {
                    PypiMetadataKey pypiMetadataKey = field.getAnnotation(PypiMetadataKey.class);
                    field.set(object, keyValueMap.get(pypiMetadataKey.name()));
                }
            }
            catch (IllegalAccessException ex)
            {
                logger.error("Exception occurred {} ...", ExceptionUtils.getStackTrace(ex));
                throw ex;
            }
        }
        return object;
    }

    private Map<String, String> generateKeyValueMap(InputStream is)
            throws IOException
    {
        Map<String, String> keyValueMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        BufferedReader reader;
        try
        {
            reader = new BufferedReader(new InputStreamReader(is));

            String line = "";
            while ((line = reader.readLine()) != null)
            {
                String[] keysValues = line.split(":", 2);
                keyValueMap.put(keysValues[0].trim(), keysValues[1].trim());
            }
            reader.close();
        }
        catch (IOException ex)
        {
            logger.error("Exception occured {} ...", ExceptionUtils.getStackTrace(ex));
            throw ex;
        }
        return keyValueMap;
    }


}
