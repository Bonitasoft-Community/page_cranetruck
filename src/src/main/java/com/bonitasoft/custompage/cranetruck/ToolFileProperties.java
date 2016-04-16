/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 */
package com.bonitasoft.custompage.cranetruck;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.bonitasoft.custompage.cranetruck.Toolbox.StatusOperation;

/**
 * @author Philippe Ozil
 */
public class ToolFileProperties extends java.util.Properties
{

    private static final long serialVersionUID = 1;
    private static String fileName;

    static Logger logger = Logger.getLogger(ToolFileProperties.class.getName());

    /**
     * Creates a Property object on a file and load it
     *
     * @param filePath
     * @throws Exception if file not found
     */
    public ToolFileProperties(final String filePath) throws Exception
    {
        super();
        final File configFile = new File(filePath);
        if (!configFile.exists()) {
            throw new Exception("Could not load configuration file: " + filePath);
        }
        fileName = configFile.getName();
        final FileInputStream input = new FileInputStream(configFile);
        this.load(input);
        input.close();
    }

    /**
     * Gets a property value of enumerated type
     *
     * @param propertyName
     * @param isMandatory
     * @param defaultValue
     * @param acceptedValueLabels list of accepted value labels
     * @return
     * @throws Exception if invalid value is specified of property is missing while being set as mandatory
     */
    public int getEnumeratedProperty(final String propertyName, final boolean isMandatory, final int defaultValue, final String[] acceptedValueLabels)
            throws Exception
    {
        // Get property value
        String propertyValue = null;
        if (isMandatory) {
            propertyValue = getProperty(propertyName);
        } else
        {
            propertyValue = getProperty(propertyName);
            if (propertyValue == null) {
                return defaultValue;
            }
        }
        propertyValue = propertyValue.trim().toLowerCase();
        // Check for valid value
        boolean isValidValue = false;
        int value = 0;
        for (int i = 0; !isValidValue && i < acceptedValueLabels.length; i++)
        {
            isValidValue = acceptedValueLabels[i].equals(propertyValue);
            if (isValidValue) {
                value = i;
            }
        }
        if (isValidValue) {
            return value;
        } else
        {
            String errorMessage = "Invalid value for property '" + propertyName + "': " + propertyValue + "\n Accepted values: ";
            for (int i = 0; i < acceptedValueLabels.length; i++) {
                errorMessage += acceptedValueLabels[i] + " ";
            }
            throw new Exception(errorMessage);
        }
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Properties file operation */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    public interface PropertiesParam {

        public boolean isSavedPropertiesFile();

        public void writeInProperties(final Properties properties);

        public void readFromProperties(final Properties properties);

        public String getPropertiesFileName();

        public void addError(String error);

        public StatusOperation getStatusOperation();

        public String getTitle();

        public Map<String, Object> toMap();
    }

    /**
     * @param propertiesParam
     * @param ldapSynchronizerPath
     * @param domain
     */
    public static void readPropertiesFile(final PropertiesParam propertiesParam, final String ldapSynchronizerPath, final String domain)
    {
        logger.info("ToolSynchronizePropertie.ReadPropertiesFile [" + propertiesParam.getTitle() + "] from file [" + propertiesParam.getPropertiesFileName()
                + "]");
        try
        {
            final ToolFileProperties properties = new ToolFileProperties(Toolbox.getConfigFileName(ldapSynchronizerPath, domain,
                    propertiesParam.getPropertiesFileName()));
            propertiesParam.readFromProperties(properties);
            logger.info("ToolSynchronizePropertie.ReadPropertiesFile [" + propertiesParam.getTitle() + "] file [" + propertiesParam.getPropertiesFileName()
                    + "] : "
                    + propertiesParam.toString());

        } catch (final Exception e)
        {
            logger.severe("ToolSynchronizePropertie.ReadPropertiesFile Error while read Properties file [" + propertiesParam.getPropertiesFileName() + "] : "
                    + e.toString());

            propertiesParam.addError("While read [" + Toolbox.getConfigFileName(ldapSynchronizerPath, domain, propertiesParam.getPropertiesFileName())
                    + "] :"
                    + e.toString());
        }
        return;

    }

    public static StatusOperation writePropertiesFile(final PropertiesParam propertiesParam, final String ldapSynchronizerPath,
            final String domain)
    {
        logger.info("CraneTruck.ToolFileProperties.writePropertiesFile: save " + propertiesParam.getClass().getName() + " to ["
                + propertiesParam.getPropertiesFileName() + "]");
        final StatusOperation statusOperation = new StatusOperation("LdapConnection");

        if (!propertiesParam.isSavedPropertiesFile())
        {
            logger.info("CraneTruck.ToolFileProperties.writePropertiesFile: don't save " + propertiesParam.getClass().getName() + " to ["
                    + propertiesParam.getPropertiesFileName() + "]");
            return statusOperation;
        }
        if (propertiesParam.getStatusOperation().isError()) {
            logger.info("CraneTruck.ToolFileProperties.writePropertiesFile: ERROR, can't saved " + propertiesParam.getClass().getName() + " to ["
                    + propertiesParam.getPropertiesFileName() + "]");
            return propertiesParam.getStatusOperation();
        }

        try
        {
            final Properties properties = new Properties();
            propertiesParam.writeInProperties(properties);

            final File f = new File(Toolbox.getConfigFileName(ldapSynchronizerPath, domain, propertiesParam.getPropertiesFileName()));
            final OutputStream out = new FileOutputStream(f);
            properties.store(out, "Save from the custom page CraneTruck.");
            statusOperation.mStatusinfo = propertiesParam.getTitle() + " Saved;";

        } catch (final Exception e)
        {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            statusOperation.mStatusError = "Can't write " + e.toString();
            logger.severe("CraneTruck.ToolFileProperties.writePropertiesFile: error at save " + e.toString() + " at " + sw.toString());
        }
        return statusOperation;
    }

    /**
     * read in the property a list of item.
     * Example
     * ldap_watched_directories = dir1,dir2
     * # Specify dir1 settings
     * dir1.ldap_search_dn = ou=people,dc=bonita,dc=com
     * dir1.ldap_search_filter = cn=*
     * # Specify dir2 settings
     * dir2.ldap_search_dn = ou=OtherPeople,dc=bonita,dc=com
     * dir2.ldap_search_filter = cn=*
     *
     * @param attributeListName : in the example, ldap_watched_directories
     * @param listOfSubAttributeName in the example [ "ldap_search_dn","ldap_search_filter" ]
     * @param properties
     * @return
     */
    public static List<Map<String, String>> readListInPropertiesFile(final String attributeListName, final String[] listOfSubAttributeName,
            final Properties properties)
    {
        final ArrayList<Map<String, String>> listResult = new ArrayList<Map<String, String>>();
        final String valueList = properties.getProperty(attributeListName);
        final StringTokenizer st = new StringTokenizer(valueList, ",");
        while (st.hasMoreTokens())
        {
            final HashMap<String, String> newItem = new HashMap<String, String>();
            listResult.add(newItem);
            String nameItem = st.nextToken();
            nameItem = nameItem.trim();
            newItem.put("name", nameItem);
            for (final String attribut : listOfSubAttributeName)
            {
                final String valueAttribut = properties.getProperty(nameItem + "." + attribut);
                newItem.put(attribut, valueAttribut);
            }
        }
        return listResult;
    }

    /**
     * do the opposite
     *
     * @param propertyName
     * @param listResult
     * @param properties
     */

    public static void writeListInPropertiesFile(final String attributeListName, final List<Map<String, String>> listResult, final String prefixName,
            final Properties properties)
    {

        String valueAttributeListName = "";
        for (int i = 0; i < listResult.size(); i++)
        {
            if (i > 0) {
                valueAttributeListName += ",";
            }
            valueAttributeListName += prefixName + i;
            for (final String attName : listResult.get(i).keySet())
            {
                if (listResult.get(i).get(attName) == null) {
                    logger.info("ToolFileProperties.WriteListInPropertiesFile: list ["+attributeListName+"] attName["+attName+"] is null index["+i+"]");
                    properties.setProperty(prefixName + i + "." + attName, "");
                } else {
                    properties.setProperty(prefixName + i + "." + attName, listResult.get(i).get(attName));
                }
            }
        }
        properties.setProperty(attributeListName, valueAttributeListName);

    }

}
