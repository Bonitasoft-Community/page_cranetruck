package com.bonitasoft.custompage.cranetruck;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.PlatformLoginAPI;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.home.BonitaHome;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.platform.PlatformLoginException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.PlatformSession;
import org.json.simple.JSONValue;

import com.bonitasoft.custompage.cranetruck.ToolFileProperties.PropertiesParam;
import com.bonitasoft.custompage.cranetruck.Toolbox.StatusOperation;
import com.bonitasoft.engine.api.LoginAPI;
import com.bonitasoft.engine.api.PlatformAPI;
import com.bonitasoft.engine.api.PlatformAPIAccessor;
import com.bonitasoft.engine.api.TenantAPIAccessor;
import com.bonitasoft.engine.api.TenantIsPausedException;
import com.bonitasoft.engine.platform.TenantNotFoundException;

public class PropertiesBonitaConnection implements PropertiesParam {

    static Logger logger = Logger.getLogger(PropertiesBonitaConnection.class.getName());
    public static final String DEFAULT_TENANT = "default";

    public String mTechnicalUser;
    public String mTechnicalPassword;
    public String mDomain;
    public String mLogin;
    public String mPassword;
    public String mBonitahome;

    private final StatusOperation mStatusOperation;

    public PropertiesBonitaConnection()
    {
        mStatusOperation = new StatusOperation("Bonita Connection");
    }

    public String getTitle()
    {
        return mStatusOperation.mStatusTitle;
    }

    /**
     * @param jsonSt
     * @return
     */
    public static PropertiesBonitaConnection getInstanceFromJsonSt(final String jsonSt) {
        if (jsonSt == null) {
            return new PropertiesBonitaConnection();
        }
        logger.info("LdapConnectionParam: JsonSt[" + jsonSt + "]");
        final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
        if (jsonHash == null) {
            return new PropertiesBonitaConnection();
        }
        return getInstanceFromMap(jsonHash);
    }

    public static PropertiesBonitaConnection getInstanceFromMap(final Map<String, Object> map) {
        final PropertiesBonitaConnection bonitaConnectionParam = new PropertiesBonitaConnection();
        if (map == null)
        {
            bonitaConnectionParam.mStatusOperation.mStatusError = "No parameters from the map";
            return bonitaConnectionParam;
        }
        bonitaConnectionParam.mTechnicalUser = Toolbox.getString(map.get("technicaluser"), null);
        bonitaConnectionParam.mTechnicalPassword = Toolbox.getString(map.get("technicalpassword"), null);
        bonitaConnectionParam.mDomain = Toolbox.getString(map.get("domain"), null);
        bonitaConnectionParam.mLogin = Toolbox.getString(map.get("login"), null);
        bonitaConnectionParam.mPassword = Toolbox.getString(map.get("password"), null);
        bonitaConnectionParam.mBonitahome = Toolbox.getString(map.get("bonitahome"), null);
        bonitaConnectionParam.checkErrors();

        return bonitaConnectionParam;
    }

    /**
     * instance the object from the properties file
     */
    public static PropertiesBonitaConnection getInstanceFromProperties(final Properties properties) {
        final PropertiesBonitaConnection bonitaConnectionParam = new PropertiesBonitaConnection();

        bonitaConnectionParam.readFromProperties(properties);
        bonitaConnectionParam.checkErrors();
        return bonitaConnectionParam;
    }

    public void readFromProperties(final Properties properties) {

        mTechnicalUser = properties.getProperty(PROP_TECH_USER);
        mTechnicalPassword = properties.getProperty(PROP_TECH_PASSWORD);

        mLogin = properties.getProperty(PROP_LOGIN);
        mPassword = properties.getProperty(PROP_PASSWORD);
        mBonitahome = properties.getProperty(PROP_BONITA_HOME);
        mStatusOperation.mStatusinfo = "Properties BonitaConnection loaded";
    }

    /**
     * set the properties File
     *
     * @param properties
     */
    public void writeInProperties(final Properties properties)
    {
        properties.setProperty(PROP_TECH_USER, mTechnicalUser);
        properties.setProperty(PROP_TECH_PASSWORD, mTechnicalPassword);
        properties.setProperty(PROP_LOGIN, mLogin);
        properties.setProperty(PROP_PASSWORD, mPassword);
        properties.setProperty(PROP_BONITA_HOME, mBonitahome == null ? "" : mBonitahome);
    }

    public boolean isSavedPropertiesFile() {
        return true;
    };

    public String getPropertiesFileName() {
        return CONFIG_FILE_NAME;
    }

    public void addError(final String error) {
        mStatusOperation.mStatusError += error + ";";

    }

    public StatusOperation checkErrors() {
        mStatusOperation.mStatusError = "";
        if (mTechnicalUser == null) {
            mStatusOperation.mStatusError = "technicalUser is mandatory;";
        }

        return mStatusOperation;
    }

    public StatusOperation getStatusOperation()
    {
        return mStatusOperation;
    };

    public HashMap<String, Object> toMap()
    {
        final HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("technicaluser", mTechnicalUser);
        result.put("technicalpassword", mTechnicalPassword);
        result.put("domain", mDomain);
        result.put("login", mLogin);
        result.put("password", mPassword);
        result.put("bonitahome", mBonitahome);
        return result;
    }

    @Override
    public String toString()
    {
        return "technicalUser[" + mTechnicalUser + "] technicalPassword[" + mTechnicalPassword + "] domain[" + mDomain + "] login[" + mLogin
                + "] password[" + mPassword + "] bonitahome[" + mBonitahome + "]";
    }

    /**
     * study this platform to retrieve the information
     *
     * @return
     */
    public static PropertiesBonitaConnection getDefaultValue(final APISession session, final IdentityAPI identityAPI)
    {
        final PropertiesBonitaConnection bonitaConnectionParam = new PropertiesBonitaConnection();
        bonitaConnectionParam.mBonitahome = System.getProperty(BonitaHome.BONITA_HOME);
        bonitaConnectionParam.mDomain = session.getTenantId() == 1 ? DEFAULT_TENANT : String.valueOf(session.getTenantId());
        bonitaConnectionParam.mLogin = "install";
        bonitaConnectionParam.mPassword = "install";
        bonitaConnectionParam.mTechnicalUser = "platformAdmin";
        bonitaConnectionParam.mTechnicalPassword = "platform";
        return bonitaConnectionParam;

    }

    /**
     * @param bonitaConnectionParam
     * @return
     */
    public StatusOperation checkBonitaConnection() {
        logger.info("Start checkBonitaConnection bonitaConnectionParam=" + toString());

        final String previousBonitaHome = System.getProperty(BonitaHome.BONITA_HOME);
        final StatusOperation statusOperation = new StatusOperation("Check BonitaConnection");

        try {
        	// Bonita home not exist anymore after 7.3 so it's not an error
        	if (mBonitahome!=null && mBonitahome.length()>0)
        	{
	            final File fileBonitaHome = new File(mBonitahome);
	            if (!fileBonitaHome.exists())
	            {
	                statusOperation.mStatusError = "BonitaHome path[" + mBonitahome + "] does not exist.";
	                return statusOperation;
	            }
	            final File fileBonitaHomeClient = new File(mBonitahome + File.separator + "client" + File.separator + "conf" + File.separator
	                    + "bonita-client.properties");
	            if (!fileBonitaHomeClient.exists())
	            {
	                statusOperation.mStatusError = "BonitaHome path[" + mBonitahome + "] is not a BonitaHome directory.";
	                return statusOperation;
	            }
	            System.setProperty(BonitaHome.BONITA_HOME, fileBonitaHome.getAbsolutePath());
        	}
        	// Test login and admin rights
            final PlatformLoginAPI platformLoginAPI = PlatformAPIAccessor.getPlatformLoginAPI();
            final PlatformSession session = platformLoginAPI.login(mTechnicalUser, mTechnicalPassword);

            final PlatformAPI platformAPI = PlatformAPIAccessor.getPlatformAPI(session);
            Long idTenant;
            // Get tenant id, and activate it if not active

            if (mDomain==null || mDomain.length()==0 || DEFAULT_TENANT.equals(mDomain)) {
                idTenant = platformAPI.getDefaultTenant().getId();
            } else {
                idTenant = platformAPI.getTenantByName(mDomain).getId();
            }

            final LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
            final APISession apiSession = loginAPI.login(idTenant, mLogin, mPassword);

            final boolean isAdmin = apiSession.isTechnicalUser();
            if (!isAdmin) {
                statusOperation.mStatusError = "The user '" + mLogin + "' is not the technical user.";
            } else {
                statusOperation.mStatusinfo = "Ok";
            }

        } catch (final TenantNotFoundException e) {
            statusOperation.mStatusError = "The Domain (tenant) '" + mDomain + "' not exists. Please create it and start again.";
        } catch (final BonitaHomeNotSetException e) {
            statusOperation.mStatusError = "The bonitahome directory [" + mBonitahome + "] is incorrect.";

        } catch (final ServerAPIException e) {
            statusOperation.mStatusError = "ServerAPI Exception  [" + e.toString() + "] .";
        } catch (final UnknownAPITypeException e) {
            statusOperation.mStatusError = "ServerAPI Exception  [" + e.toString() + "] .";
        } catch (final PlatformLoginException e) {
            statusOperation.mStatusError = "Platfom login exception with login [" + mTechnicalUser + "]/password["
                    + mTechnicalPassword + "]";
        } catch (final TenantIsPausedException e) {
            statusOperation.mStatusError = "Tenant is paused  [" + e.toString() + "] .";
        } catch (final LoginException e) {
            statusOperation.mStatusError = "Login exception with login [" + mLogin + "]/password[" + mPassword
                    + "]";
        }
        catch(Exception e)
        {
            statusOperation.mStatusError = "Error ["+e.toString()+"]";
                    
        }

        
        if (previousBonitaHome != null) {
            System.setProperty(BonitaHome.BONITA_HOME, previousBonitaHome);
        }

        return statusOperation;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Properties file */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    private static final String CONFIG_FILE_NAME = "bonita.properties";

    // Configuration properties
    private static final String PROP_LOGIN = "login";

    private static final String PROP_PASSWORD = "password";

    private static final String PROP_TECH_USER = "technicalUser";

    private static final String PROP_TECH_PASSWORD = "technicalPassword";

    private static final String PROP_BONITA_HOME = "bonita_home";

    /*
     * public static BonitaConnectionParam readPropertiesFile(final String ldapSynchronizerPath, final String domain)
     * {
     * BonitaConnectionParam bonitaConnectionParam = null;
     * try
     * {
     * final ToolSynchronizerProperties properties = new ToolSynchronizerProperties(Toolbox.getConfigFileName(ldapSynchronizerPath, domain, CONFIG_FILE_NAME));
     * bonitaConnectionParam = BonitaConnectionParam.getInstanceFromProperties(properties);
     * bonitaConnectionParam.mDomain = domain;
     * } catch (final Exception e)
     * {
     * final StringWriter sw = new StringWriter();
     * e.printStackTrace(new PrintWriter(sw));
     * logger.severe("LdapConnection:" + e.toString() + " detail: " + sw.toString());
     * bonitaConnectionParam = new BonitaConnectionParam();
     * bonitaConnectionParam.mStatusOperation.mStatusError = "While read [" + Toolbox.getConfigFileName(ldapSynchronizerPath, domain, CONFIG_FILE_NAME)
     * + "] :"
     * + e.toString();
     * }
     * return bonitaConnectionParam;
     * }
     * /**
     * @param bonitaConnectionMap
     * @param ldapSynchronizerPath
     * @param domain
     * @return
     * public static StatusOperation writePropertiesFileFromMap(final Map<String, Object> bonitaConnectionMap, final String ldapSynchronizerPath,
     * final String domain)
     * {
     * final HashMap<String, Object> result = new HashMap<String, Object>();
     * final BonitaConnectionParam bonitaConnectionParam = BonitaConnectionParam.getInstanceFromMap(bonitaConnectionMap);
     * if (bonitaConnectionParam.getStatusOperation().isError()) {
     * logger.info("BonitaConnection.writePropertiesFileFromMap: error [" + bonitaConnectionParam.getStatusOperation().mStatusError + "]");
     * return bonitaConnectionParam.getStatusOperation();
     * }
     * return writePropertiesFile(bonitaConnectionParam, ldapSynchronizerPath, domain);
     * }
     * /**
     * write the properties file
     * @param bonitaConnectionParam
     * @param ldapSynchronizerPath
     * @param domain
     * @return
     * public static StatusOperation writePropertiesFile(final BonitaConnectionParam bonitaConnectionParam, final String ldapSynchronizerPath,
     * final String domain)
     * {
     * logger.info("CraneTruck.bonitaConnection.writePropertiesFile: save");
     * final StatusOperation statusOperation = new StatusOperation("BonitaConnection");
     * try
     * {
     * final Properties properties = new Properties();
     * bonitaConnectionParam.setProperties(properties);
     * final File f = new File(Toolbox.getConfigFileName(ldapSynchronizerPath, domain, CONFIG_FILE_NAME));
     * final OutputStream out = new FileOutputStream(f);
     * properties.store(out, "Save from the custom page CraneTruck.");
     * statusOperation.mStatusinfo = "OK";
     * } catch (final Exception e)
     * {
     * statusOperation.mStatusError = "Can't write " + e.toString();
     * logger.severe("CraneTruck.bonitaConnection.writePropertiesFile: error at save " + e.toString());
     * }
     * return statusOperation;
     * }
     */

}
