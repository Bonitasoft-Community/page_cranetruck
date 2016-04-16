import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.Runtime;

import org.json.simple.JSONObject;
import org.codehaus.groovy.tools.shell.CommandAlias;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;


import javax.naming.Context;
import javax.naming.InitialContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;

import org.apache.commons.lang3.StringEscapeUtils

import org.bonitasoft.engine.identity.User;
import org.bonitasoft.console.common.server.page.PageContext
import org.bonitasoft.console.common.server.page.PageController
import org.bonitasoft.console.common.server.page.PageResourceProvider
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;

import com.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import com.bonitasoft.engine.api.PlatformMonitoringAPI;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchResult;

import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandCriterion;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
	
	
import com.bonitasoft.custompage.cranetruck.PropertiesLdapConnection;
import com.bonitasoft.custompage.cranetruck.PropertiesBonitaConnection;
import com.bonitasoft.custompage.cranetruck.PropertiesLogger;
import com.bonitasoft.custompage.cranetruck.PropertiesMapper;
import com.bonitasoft.custompage.cranetruck.PropertiesSynchronize;
import com.bonitasoft.custompage.cranetruck.PropertiesSynchronize.PropertiesSynchronizeTest;

import com.bonitasoft.custompage.cranetruck.CraneTruckAccess;
import com.bonitasoft.custompage.cranetruck.CraneTruckAccess.CraneTruckParam;
import com.bonitasoft.custompage.cranetruck.JaasCheck;
import com.bonitasoft.custompage.cranetruck.LdapLoginModuleCheck;
import com.bonitasoft.custompage.cranetruck.UsersOperation;


public class Index implements PageController {



	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
	
		Logger logger= Logger.getLogger("org.bonitasoft");
		
		
		try {
			def String indexContent;
			pageResourceProvider.getResourceAsStream("Index.groovy").withStream { InputStream s-> indexContent = s.getText() };
			response.setCharacterEncoding("UTF-8");
			PrintWriter out = response.getWriter()

			String action=request.getParameter("action");
			logger.info("pt1");
			String json = request.getParameter("json");
			logger.info("###################################### action is["+action+"] json=["+json+"] !");
			if (action==null || action.length()==0 )
			{
				logger.severe(">run Default !");
				
				runTheBonitaIndexDoGet( request, response,pageResourceProvider,pageContext);
				return;
			}
			
			APISession session = pageContext.getApiSession()
			ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(session);
			PlatformMonitoringAPI platformMonitoringAPI = TenantAPIAccessor.getPlatformMonitoringAPI(session);
			IdentityAPI identityApi = TenantAPIAccessor.getIdentityAPI(session);
			
			HashMap<String,Object> answer = null;
			if ("readfromproperties".equals(action))
			{
				CraneTruckParam craneTruckParam = CraneTruckParam.getInstanceFromJsonSt( json );
				answer =  CraneTruckAccess.readFromProperties(craneTruckParam);
			}
			else if ("writetoproperties".equals(action))
			{
				CraneTruckParam craneTruckParam = CraneTruckParam.getInstanceFromJsonSt( json );
				answer =  CraneTruckAccess.writeToProperties(json,craneTruckParam);
			}			
			else if ("testsynchronize".equals(action))
			{
				PropertiesSynchronizeTest synchronizeTest = PropertiesSynchronize.PropertiesSynchronizeTest.getInstanceFromJsonSt( json );
				answer = PropertiesSynchronize.checkSynchronize( synchronizeTest ).toMap();
			}
			
			else if ("testldapconnection".equals(action))
			{
				PropertiesLdapConnection ldapConnection = PropertiesLdapConnection.getInstanceFromJsonSt( json );
				answer = ldapConnection.checkLdapConnection().toMap();
			}					
			else if ("getdefaultbonitaconnection".equals(action))
			{
				answer = PropertiesBonitaConnection.getDefaultValue(session, identityApi).toMap();
			}
			else if ("testbonitaconnection".equals(action))
			{
				PropertiesBonitaConnection bonitaConnection = PropertiesBonitaConnection.getInstanceFromJsonSt( json );
				answer = bonitaConnection.checkBonitaConnection().toMap();
			}
			
			else if ("testjaasconnection".equals(action))
			{
			    // parama is given in the URL : so the & was encode _£
				String jsonStReplace = json.replace("_£", "&");

				JaasCheck jaasCheck = JaasCheck.getInstanceFromJsonSt( jsonStReplace );
				answer = jaasCheck.checkJaasConnection().toMap();
			}
			else if ("testldaploginmodule".equals(action))
			{
				 // parama is given in the URL : so the & was encode _£
				String jsonStReplace = json.replace("_£", "&");
				
				LdapLoginModuleCheck ldapLoginModuleCheck = LdapLoginModuleCheck.getInstanceFromJsonSt( jsonStReplace );
				answer = ldapLoginModuleCheck.checkLdapLoginModuleConnection( ).toMap();
	
			}
			else if ("getjaasenvironment".equals(action))
			{
				String jsonStReplace = json.replace("_£", "&");
				JaasCheck jaasCheck = JaasCheck.getInstanceFromJsonSt( jsonStReplace );
				answer = jaasCheck.getEnvironnementJaasConnection( ).toMap();
			}
            else if ("usersgetlist".equals(action))
            {
                String jsonStReplace = json.replace("_£", "&");
                UsersOperation userOperations = UsersOperation.getInstanceFromJsonSt( jsonStReplace );
                answer = userOperations.getUsersList( identityApi ).toMap();
            }
            else if ("usersdooperation".equals(action))
            {
                String jsonStReplace = json.replace("_£", "&");
                UsersOperation userOperations = UsersOperation.getInstanceFromJsonSt( jsonStReplace );
                userOperations.simulateOperation=false;
                answer = userOperations.doOperation( identityApi, session.userId ).toMap();
            }
            
            
			if (answer != null)
			{			
				String jsonDetailsSt = JSONValue.toJSONString( answer );
				logger.info("Index.groovy return Json="+jsonDetailsSt);
				
				out.write( jsonDetailsSt );
				out.flush();
				out.close();				
				return;				
			}
			
			out.write( "Unknow command" );
			out.flush();
			out.close();
			return;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			logger.severe("Exception ["+e.toString()+"] at "+exceptionDetails);
		}
	}

	
	/** -------------------------------------------------------------------------
	 *
	 *runTheBonitaIndexDoGet
	 * 
	 */
	private void runTheBonitaIndexDoGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
				try {
						def String indexContent;
						pageResourceProvider.getResourceAsStream("index.html").withStream { InputStream s->
								indexContent = s.getText()
						}
						
						def String pageResource="pageResource?&page="+ request.getParameter("page")+"&location=";
						
						// Living application : do not replace						
						// indexContent= indexContent.replace("@_USER_LOCALE_@", request.getParameter("locale"));
						// indexContent= indexContent.replace("@_PAGE_RESOURCE_@", pageResource);
						
						response.setCharacterEncoding("UTF-8");
						PrintWriter out = response.getWriter();
						out.print(indexContent);
						out.flush();
						out.close();
				} catch (Exception e) {
						e.printStackTrace();
				}
		}
		
		
}
