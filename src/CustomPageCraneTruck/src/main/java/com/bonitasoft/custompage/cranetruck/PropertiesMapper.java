package com.bonitasoft.custompage.cranetruck;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.json.simple.JSONValue;

import com.bonitasoft.custompage.cranetruck.ToolFileProperties.PropertiesParam;
import com.bonitasoft.custompage.cranetruck.Toolbox.StatusOperation;

public class PropertiesMapper implements PropertiesParam {

    static Logger logger = Logger.getLogger(PropertiesLdapConnection.class.getName());

    private final ArrayList<HashMap<String, String>> listMappingAttributes;
    private final static String MAPPING_BONITANAME = "b";
    private final static String MAPPING_LDAPNAME = "l";
    private final static String MAPPING_EXAMPLE = "e";

    private final StatusOperation mStatusOperation;

    public PropertiesMapper()
    {
        mStatusOperation = new StatusOperation("Mapper");
        listMappingAttributes = new ArrayList<HashMap<String, String>>();
        listMappingAttributes.add(getHashMap("user_name", "uid"));
        listMappingAttributes.add(getHashMap("first_name", "givenName"));
        listMappingAttributes.add(getHashMap("last_name", "sn"));
        listMappingAttributes.add(getHashMap("title", "title"));
        listMappingAttributes.add(getHashMap("job_title", ""));
        listMappingAttributes.add(getHashMap("manager", ""));
        listMappingAttributes.add(getHashMap("delegee", ""));
        listMappingAttributes.add(getHashMap("pro_email", "mail"));
        listMappingAttributes.add(getHashMap("pro_phone", "telephoneNumber"));
        listMappingAttributes.add(getHashMap("pro_mobile", "mobile"));
        listMappingAttributes.add(getHashMap("pro_fax", ""));
        listMappingAttributes.add(getHashMap("pro_website", ""));
        listMappingAttributes.add(getHashMap("pro_room", ""));
        listMappingAttributes.add(getHashMap("pro_building", ""));
        listMappingAttributes.add(getHashMap("pro_address", "postalAddress"));
        listMappingAttributes.add(getHashMap("pro_city", ""));
        listMappingAttributes.add(getHashMap("pro_zip_code", "postalCode"));
        listMappingAttributes.add(getHashMap("pro_state", ""));
        listMappingAttributes.add(getHashMap("pro_country", ""));
        listMappingAttributes.add(getHashMap("perso_email", ""));
        listMappingAttributes.add(getHashMap("perso_phone", ""));
        listMappingAttributes.add(getHashMap("perso_mobile", ""));
        listMappingAttributes.add(getHashMap("perso_fax", ""));
        listMappingAttributes.add(getHashMap("perso_website", ""));
        listMappingAttributes.add(getHashMap("perso_room", ""));
        listMappingAttributes.add(getHashMap("perso_building", ""));
        listMappingAttributes.add(getHashMap("perso_address", ""));
        listMappingAttributes.add(getHashMap("perso_city", ""));
        listMappingAttributes.add(getHashMap("perso_zip_code", ""));
        listMappingAttributes.add(getHashMap("perso_state", ""));
        listMappingAttributes.add(getHashMap("perso_country", ""));

    }

    private HashMap<String, String> getHashMap(final String bonitaName, final String example)
    {
        final HashMap<String, String> record = new HashMap<String, String>();
        record.put(MAPPING_BONITANAME, bonitaName);
        record.put(MAPPING_EXAMPLE, example);
        return record;
    }

    public String getTitle()
    {
        return mStatusOperation.mStatusTitle;
    }

    /**
     * @param jsonSt
     * @return
     */
    public static PropertiesMapper getInstanceFromJsonSt(final String jsonSt) {
        final PropertiesMapper ldapConnectionParam = new PropertiesMapper();
        if (jsonSt == null) {
            return ldapConnectionParam;
        }
        logger.info("LdapConnectionParam: JsonSt[" + jsonSt + "]");
        final Map<String, Object> jsonHash = (Map<String, Object>) JSONValue.parse(jsonSt);
        if (jsonHash == null) {
            return ldapConnectionParam;
        }
        return getInstanceFromMap(jsonHash);
    }

    /**
     * @param map
     * @return
     */
    public static PropertiesMapper getInstanceFromMap(final Map<String, Object> map) {
        final PropertiesMapper propertiesMapper = new PropertiesMapper();
        if (map == null)
        {
            propertiesMapper.mStatusOperation.mStatusError = "No parameters from the map";
            return propertiesMapper;
        }

        logger.info("PropertiesMapper.getInstanceFromMap[" + map.toString() + "]");
        propertiesMapper.listMappingAttributes.clear();
        final List<HashMap<String, String>> listAttributes = (List<HashMap<String, String>>) map.get("listattributes");
        for (final HashMap<String, String> oneRecord : listAttributes)
        {
            propertiesMapper.listMappingAttributes.add(oneRecord);
        }
        logger.info("After the getInstanceFromMap[" + propertiesMapper.listMappingAttributes.toString() + "]");
        propertiesMapper.checkErrors();

        return propertiesMapper;
    }

    /**
     *
     */
    public Map<String, Object> toMap()
    {
        final HashMap<String, Object> result = new HashMap<String, Object>();

        // format is "bonitaname: <key> ldapname:<key> example:<key>

        result.put("listattributes", listMappingAttributes);

        return result;
    }

    /**
     * instance the object from the properties file
     */
    public static PropertiesMapper getInstanceFromProperties(final Properties properties) {
        final PropertiesMapper ldapConnectionParam = new PropertiesMapper();
        ldapConnectionParam.readFromProperties(properties);

        ldapConnectionParam.checkErrors();
        return ldapConnectionParam;
    }

    /**
     * write values in the properties file
     */
    public void writeInProperties(final Properties properties)
    {
        for (final HashMap<String, String> mappingAttribut : listMappingAttributes)
        {
            if (mappingAttribut.get(MAPPING_LDAPNAME) != null)
            {
                properties.setProperty(mappingAttribut.get(MAPPING_BONITANAME), mappingAttribut.get(MAPPING_LDAPNAME));

            }
        }

    }

    public boolean isSavedPropertiesFile() {
        return true;
    };

    /**
     * check the parameters, and return a status
     *
     * @return
     */
    public StatusOperation checkErrors() {
        mStatusOperation.mStatusError = "";

        return mStatusOperation;
    }

    public StatusOperation getStatusOperation()
    {
        return mStatusOperation;
    };

    @Override
    public String toString()
    {
        return "LoggerProperties:";
    }


    public void readFromProperties(final Properties properties) {
        for (final Object key : properties.keySet()) {
            // search the key in the list
            boolean found = false;
            for (final HashMap<String, String> record : listMappingAttributes)
            {
                if (record.get(MAPPING_BONITANAME).equals(key)) {
                    record.put(MAPPING_LDAPNAME, properties.getProperty(key.toString()));
                    found = true;
                }
            }
            if (!found)
            {
                final HashMap<String, String> newRecord = new HashMap<String, String>();
                newRecord.put(MAPPING_BONITANAME, key.toString());
                newRecord.put(MAPPING_LDAPNAME, properties.getProperty(key.toString()));
                listMappingAttributes.add(newRecord);
                mStatusOperation.addError("Properties[" + key.toString() + "] unknown.");
            }
        }
        mStatusOperation.mStatusinfo = "Properties Mapper loaded";
    }

    public String getPropertiesFileName() {
        return CONFIG_FILE_NAME;
    }

    public void addError(final String error) {
        mStatusOperation.mStatusError += error + ";";

    }

    /***
     * check check the Ldap
     *
     * @param mHostURL
     * @param mAuthType
     * @param mPrincipalDN
     * @param mPassword
     * @param mSearchDN
     * @param mSearchFilter
     * @return
     */
    public StatusOperation checkLoggerConnection() {
        logger.info("Start checkLogger=" + toString());

        final HashMap<String, Object> result = new HashMap<String, Object>();
        final StatusOperation statusOperation = getStatusOperation();
        if (statusOperation.isError())
        {
            return statusOperation;
        }
        final StringBuffer finalResult = new StringBuffer();
        try
        {
            statusOperation.mStatusinfo = "OK";
            return statusOperation;

        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("LdapConnection:" + e.toString() + " detail: " + sw.toString());

            finalResult.append("Exception :" + e.toString());
            statusOperation.mStatusError = "Exception " + e.toString();
            return statusOperation;
        }
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Properties file */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    private static final String CONFIG_FILE_NAME = "mapper.properties";

}
