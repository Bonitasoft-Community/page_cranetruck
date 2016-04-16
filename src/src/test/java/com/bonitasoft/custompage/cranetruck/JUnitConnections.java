package com.bonitasoft.custompage.cranetruck;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.junit.Test;

import com.bonitasoft.custompage.cranetruck.CraneTruckAccess.CraneTruckParam;
import com.bonitasoft.custompage.cranetruck.PropertiesSynchronize.PropertiesSynchronizeTest;
import com.bonitasoft.custompage.cranetruck.Toolbox.StatusOperation;

public class JUnitConnections {

    static Logger logger = Logger.getLogger(PropertiesLdapConnection.class.getName());

    // @Test
    public void testLdapConnection() {
        final PropertiesLdapConnection ldapConnectionParam = new PropertiesLdapConnection();
        ldapConnectionParam.mHostURL = "ldap://localhost:10389";
        ldapConnectionParam.mAuthType = "simple";
        ldapConnectionParam.mPrincipalDN = "uid=admin, ou=system";
        ldapConnectionParam.mPassword = "secret";
        ldapConnectionParam.mSearchDN = "dc=example,dc=com";
        ldapConnectionParam.mSearchFilter = "uid=walter.bates";

        final StatusOperation result = ldapConnectionParam.checkLdapConnection();
        System.out.println(result.toMap());
    }

    @Test
    public void testLdapModuleConnection() {
        final LdapLoginModuleCheck ldapConnectionParam = new LdapLoginModuleCheck();
        ldapConnectionParam.mUserProvider = "ldap://localhost:10389/ou=People,dc=example,dc=com";
        ldapConnectionParam.mUserFilter = "(&(uid={USERNAME})(objectClass=inetOrgPerson))";
        ldapConnectionParam.mAuthIdentity = "{USERNAME}";
        ldapConnectionParam.mUseSSL = false;
        ldapConnectionParam.mLogin = "walter.bates";
        ldapConnectionParam.mPassword = "bpm";

        final StatusOperation result = ldapConnectionParam.checkLdapLoginModuleConnection();
        System.out.println(result.toMap());
    }

    @Test
    public void testBonitaConnectionDefault()
    {
        final APISession session = login();
        if (session == null) {
            return;
        }
        IdentityAPI identityAPI;
        try {
            identityAPI = TenantAPIAccessor.getIdentityAPI(session);
            System.out.println("BonitaConnection Default=" + PropertiesBonitaConnection.getDefaultValue(session, identityAPI));

        } catch (final BonitaHomeNotSetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ServerAPIException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final UnknownAPITypeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    // @Test
    public void testBonitaConnection()
    {
        final PropertiesBonitaConnection bonitaConnectionParam = new PropertiesBonitaConnection();
        bonitaConnectionParam.mBonitahome = "C:/atelier/BPM-SP-6.4.2/workspace/tomcat/bonita";
        bonitaConnectionParam.mDomain = PropertiesBonitaConnection.DEFAULT_TENANT;
        bonitaConnectionParam.mLogin = "Walter.Bates";
        bonitaConnectionParam.mPassword = "bpm";
        bonitaConnectionParam.mTechnicalUser = "platformAdmin";
        bonitaConnectionParam.mTechnicalPassword = "platform";
        System.out.println("BonitaConnection =" + bonitaConnectionParam.checkBonitaConnection().toMap());

    }

    @Test
    public void readProperties()
    {
        final CraneTruckParam craneTruckParam = new CraneTruckParam();
        craneTruckParam.domain = "default";
        craneTruckParam.ldapSynchronizerPath = "C:/atelier/LDAP-Synchronizer 6.4.2/BonitaBPMSubscription-6.4.2-LDAP-Synchronizer/conf";

        final HashMap<String, Object> readResult = CraneTruckAccess.readFromProperties(craneTruckParam);
        System.out.println("result=" + readResult);

    }

    //  @ T e s t
    public void testSynchronisation()
    {
        final String jsonSt = "{\"statuserror\":\"\",\"title\":\"Read PropertiesLdap Connection;Bonita Connection;Logger;Sync;Mapper;\",\"error\":\"\",\"details\":\"\",\"ldap\":{\"pagesize\":1000,\"principaldn\":\"uid=admin, ou=system\",\"authtype\":\"simple\",\"used_paged_search\":\"true\",\"searchfilter\":null,\"hosturl\":\"ldap://localhost:10389\",\"directory_user_type\":\"person\",\"password\":\"secret\",\"searchdn\":null,\"used_paged_search_list\":{\"name\":\"true\",\"value\":\"true\"}},\"sync\":{\"allow_recursive_groups\":true,\"bonita_username_case_list\":{\"name\":\"lowercase\",\"value\":\"lowercase\"},\"bonita_deactivate_users\":\"true\",\"error_level_upon_failing_to_get_related_user\":\"WARNING\",\"bonita_nosync_users\":\"admin,john,james,jack\",\"ldap_searchs\":[{\"ldap_search_filter\":\"cm=A_*\",\"ldap_search_dn\":\"ou=people,dc=bonita,dc=com\"},{\"ldap_search_filter\":\"cm=B_*\",\"ldap_search_dn\":\"ou=people,dc=bonita,dc=com\"},{\"ldap_search_filter\":\"cn=newsearch\",\"ldap_search_dn\":\"ou=newsearch\"}],\"ldap_watched_directories\":[{\"ldap_search_filter\":\"cn=*\",\"ldap_search_dn\":\"ou=people,dc=bonita,dc=com\"},{\"ldap_search_filter\":\"cn=*\",\"ldap_search_dn\":\"ou=OtherPeople,dc=bonita,dc=com\"},{\"ldap_search_filter\":\"newforced\",\"ldap_search_dn\":\"ou=new\"}],\"bonita_remove_users\":\"\",\"ldap_groups\":[{\"ldap_group_dn\":\"cn=group1,ou=groups,dc=bonita,dc=com\",\"forced_bonita_group_name\":\"forced group1\"},{\"ldap_group_dn\":\"cn=group2,ou=groups,dc=bonita,dc=com\",\"forced_bonita_group_name\":\"\"},{\"ldap_group_dn\":\"cn=newgroup\",\"forced_bonita_group_name\":\"cn=forcernewgroup\"}],\"bonita_user_role\":\"user\",\"error_level_upon_failing_to_get_related_user_list\":{\"name\":\"Warning\",\"value\":\"WARNING\"},\"bonita_deactivate_users_list\":{\"name\":\"true\",\"value\":\"true\"},\"allow_recursive_groups_list\":{\"name\":\"true\",\"value\":\"true\"},\"bonita_username_case\":\"lowercase\"},\"detailsjsonmap\":null,\"mapper\":{\"listattributes\":[{\"e\":\"uid\",\"b\":\"user_name\",\"l\":\"uid\"},{\"e\":\"givenName\",\"b\":\"first_name\",\"l\":\"\"},{\"e\":\"sn\",\"b\":\"last_name\",\"l\":\"sn\"},{\"e\":\"title\",\"b\":\"title\",\"l\":\"title\"},{\"e\":\"\",\"b\":\"job_title\"},{\"e\":\"\",\"b\":\"manager\"},{\"e\":\"\",\"b\":\"delegee\"},{\"e\":\"mail\",\"b\":\"pro_email\",\"l\":\"mail\"},{\"e\":\"telephoneNumber\",\"b\":\"pro_phone\",\"l\":\"telephoneNumber\"},{\"e\":\"mobile\",\"b\":\"pro_mobile\",\"l\":\"mobile\"},{\"e\":\"\",\"b\":\"pro_fax\"},{\"e\":\"\",\"b\":\"pro_website\"},{\"e\":\"\",\"b\":\"pro_room\"},{\"e\":\"\",\"b\":\"pro_building\"},{\"e\":\"postalAddress\",\"b\":\"pro_address\"},{\"e\":\"\",\"b\":\"pro_city\"},{\"e\":\"postalCode\",\"b\":\"pro_zip_code\"},{\"e\":\"\",\"b\":\"pro_state\"},{\"e\":\"\",\"b\":\"pro_country\"},{\"e\":\"\",\"b\":\"perso_email\"},{\"e\":\"\",\"b\":\"perso_phone\",\"l\":\"homePhone\"},{\"e\":\"\",\"b\":\"perso_mobile\"},{\"e\":\"\",\"b\":\"perso_fax\"},{\"e\":\"\",\"b\":\"perso_website\"},{\"e\":\"\",\"b\":\"perso_room\"},{\"e\":\"\",\"b\":\"perso_building\"},{\"e\":\"\",\"b\":\"perso_address\"},{\"e\":\"\",\"b\":\"perso_city\"},{\"e\":\"\",\"b\":\"perso_zip_code\"},{\"e\":\"\",\"b\":\"perso_state\"},{\"e\":\"\",\"b\":\"perso_country\"}]},\"logger\":{\"log_file_date_prefix\":\"yyyy-MM-dd\",\"log_level\":\"WARNING\",\"log_dir_path\":\"logs/\",\"log_level_list\":{\"name\":\"Warning\",\"value\":\"WARNING\"}},\"bonita\":{\"technicaluser\":\"platformAdmin\",\"bonitahome\":\"\",\"domain\":null,\"login\":\"install\",\"technicalpassword\":\"platform\",\"password\":\"install\"},\"info\":\"Properties LdapConnection loaded;Properties BonitaConnection loaded;Properties Logger loaded;Properties Mapper loaded;\",\"ldapSynchronizerPath\":\"C:/atelier/LDAP-Synchronizer 6.4.2/BonitaBPMSubscription-6.4.2-LDAP-Synchronizer/conf\"}]";
        final PropertiesSynchronizeTest synchronizeTest = PropertiesSynchronize.PropertiesSynchronizeTest.getInstanceFromJsonSt(jsonSt);
        final Map<String, Object> readResult = PropertiesSynchronize.checkSynchronize(synchronizeTest).toMap();
        System.out.println("result=" + readResult);

    }

    @Test
    public void testJaasConnection()
    {
        final String json = "{\"jaascontent\":\"BonitaAuthentication-1 {\n    com.sun.security.auth.module.LdapLoginModule REQUIRED\n    userProvider=\\\"ldap://localhost:10389/ou=People\\\"\n    userFilter=\\\"(&(uid={USERNAME})(objectClass=inetOrgPerson))\\\"\n    authzIdentity=\\\"{USERNAME}\\\"\n    debug=true\n    useSSL=false;\n};\",\"jaasauthentkey\":\"BonitaAuthentication-1\",\"jaasusername\":\"walter.bates\",\"jasspassword\":\"bpm\"}";
        final JaasCheck jaasCheck = JaasCheck.getInstanceFromJsonSt(json);
        final Map<String, Object> readResult = jaasCheck.checkJaasConnection().toMap();
        System.out.println("result=" + readResult);
    }
    public APISession login()
    {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("server.url", "http://localhost:8081");
        map.put("application.name", "bonita");
        APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map);

        // Set the username and password
        // final String username = "helen.kelly";
        final String username = "walter.bates";
        final String password = "bpm";
        logger.info("userName[" + username + "]");
        logger.setLevel(Level.FINEST);
        final ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINEST);
        logger.addHandler(handler);

        logger.fine("Debug level is visible");

        // get the LoginAPI using the TenantAPIAccessor
        LoginAPI loginAPI;
        try {
            loginAPI = TenantAPIAccessor.getLoginAPI();
            // log in to the tenant to create a session
            final APISession session = loginAPI.login(username, password);
            return session;
        } catch (final Exception e)
        {
            logger.severe("during login " + e.toString());
        }
        return null;
    }

}
