import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession;
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
	
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;

import org.bonitasoft.properties.BonitaProperties;

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


public class Actions {

	private static Logger logger= Logger.getLogger("org.bonitasoft.custompage.longboard.groovy");
	
	
	public static Index.ActionAnswer doAction(HttpServletRequest request, String paramJsonSt, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
	
		Logger logger= Logger.getLogger("org.bonitasoft");
		
		Index.ActionAnswer actionAnswer = new Index.ActionAnswer();	
		
		try {
			String action=request.getParameter("action");
			logger.info("#### LongBoardCustomPage:Actions  action is["+action+"] !");
			if (action==null || action.length()==0 )
			{
				actionAnswer.isManaged=false;
				logger.info("#### LongBoardCustomPage:Actions END No Actions");
				return actionAnswer;
			}
			actionAnswer.isManaged=true;
			HttpSession httpSession = request.getSession() ;
            
		
			
			logger.info("###################################### action is["+action+"] json=["+paramJsonSt+"] !");
			
			
			APISession session = pageContext.getApiSession()
			ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(session);
			PlatformMonitoringAPI platformMonitoringAPI = TenantAPIAccessor.getPlatformMonitoringAPI(session);
			IdentityAPI identityApi = TenantAPIAccessor.getIdentityAPI(session);
			
			HashMap<String,Object> answer = null;
			if ("initpage".equals(action)) {
			  	 List<BEvent> listEvents=new ArrayList<BEvent>();
				 
				 BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider, session.getTenantId() );
	             listEvents.addAll( bonitaProperties.load() );
	             logger.info("BonitaProperties.saveConfig: loadProperties done, events = "+listEvents.size() );
	             actionAnswer.responseMap.put("ldapSynchronizerPath", bonitaProperties.get("ldapSynchronizerPath")); 	            
	             actionAnswer.responseMap.put("domain", bonitaProperties.get("domain")); 	            
 	             actionAnswer.responseMap.put("listevents", BEventFactory.getHtml(listEvents));
			}
			if ("readfromproperties".equals(action))
			{
				CraneTruckParam craneTruckParam = CraneTruckParam.getInstanceFromJsonSt( paramJsonSt );
				actionAnswer.setResponse( CraneTruckAccess.readFromProperties(craneTruckParam));
				
				// save the path 
  		      	List<BEvent> listEvents=new ArrayList<BEvent>();
			      
				BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider, session.getTenantId() );
	            listEvents.addAll( bonitaProperties.load() );
	            bonitaProperties.put("ldapSynchronizerPath", craneTruckParam.ldapSynchronizerPath); 	            
	            bonitaProperties.put("domain", craneTruckParam.domain); 	            
	            listEvents.addAll( bonitaProperties.store() );
	            actionAnswer.responseMap.put("listevents", BEventFactory.getHtml(listEvents));
	          	
			    
			}
			else if ("writetoproperties".equals(action))
			{
				CraneTruckParam craneTruckParam = CraneTruckParam.getInstanceFromJsonSt( paramJsonSt );
				actionAnswer.setResponse( CraneTruckAccess.writeToProperties(paramJsonSt,craneTruckParam));
			}			
			else if ("testsynchronize".equals(action))
			{
				PropertiesSynchronizeTest synchronizeTest = PropertiesSynchronize.PropertiesSynchronizeTest.getInstanceFromJsonSt( paramJsonSt );
				actionAnswer.setResponse( PropertiesSynchronize.checkSynchronize( synchronizeTest ).toMap() );
			}
			
			else if ("testldapconnection".equals(action))
			{
				PropertiesLdapConnection ldapConnection = PropertiesLdapConnection.getInstanceFromJsonSt( paramJsonSt );
				actionAnswer.setResponse( ldapConnection.checkLdapConnection().toMap());
			}					
			else if ("getdefaultbonitaconnection".equals(action))
			{
				actionAnswer.setResponse( PropertiesBonitaConnection.getDefaultValue(session, identityApi).toMap());
			}
			else if ("testbonitaconnection".equals(action))
			{
				PropertiesBonitaConnection bonitaConnection = PropertiesBonitaConnection.getInstanceFromJsonSt( paramJsonSt );
				actionAnswer.setResponse( bonitaConnection.checkBonitaConnection().toMap() );
			}
			
			else if ("testjaasconnection".equals(action))
			{
			    // parama is given in the URL : so the & was encode _£
				// String jsonStReplace = paramJsonSt.replace("_£", "&");
			    String accumulateJson = (String) httpSession.getAttribute("accumulate" );
		        logger.info("testjaasconnection accumulateJson=["+accumulateJson+"]");
		        
				JaasCheck jaasCheck = JaasCheck.getInstanceFromJsonSt( accumulateJson );
				actionAnswer.setResponse( jaasCheck.checkJaasConnection().toMap());
			}
			else if ("testldaploginmodule".equals(action))
			{
				 // parama is given in the URL : so the & was encode _£
				// String paramJsonSt = paramJsonSt.replace("_£", "&");
				
				LdapLoginModuleCheck ldapLoginModuleCheck = LdapLoginModuleCheck.getInstanceFromJsonSt( paramJsonSt );
				actionAnswer.setResponse( ldapLoginModuleCheck.checkLdapLoginModuleConnection( ).toMap());
	
			}
			else if ("getjaasenvironment".equals(action))
			{
				//String paramJsonSt = paramJsonSt.replace("_£", "&");
				JaasCheck jaasCheck = JaasCheck.getInstanceFromJsonSt( paramJsonSt );
				actionAnswer.setResponse( jaasCheck.getEnvironnementJaasConnection( ).toMap());
			}
            else if ("usersgetlist".equals(action))
            {
                //String paramJsonSt = paramJsonSt.replace("_£", "&");
                UsersOperation userOperations = UsersOperation.getInstanceFromJsonSt( paramJsonSt );
                actionAnswer.setResponse(userOperations.getUsersList( identityApi ).toMap());
            }
            else if ("usersdooperation".equals(action))
            {
                //String paramJsonSt = paramJsonSt.replace("_£", "&");
                UsersOperation userOperations = UsersOperation.getInstanceFromJsonSt( paramJsonSt );
                userOperations.simulateOperation=false;
                actionAnswer.setResponse( userOperations.doOperation( identityApi, session.userId ).toMap());
            }
            else if ("collect_reset".equals(action))
            {
                httpSession.setAttribute("accumulate", "" );
                actionAnswer.responseMap.put("status", "ok");
                logger.info("collect_reset");
                
            }
            else if ("collect_add".equals(action))
            {
                String paramJsonPartial = request.getParameter("paramjsonpartial");
                logger.info("collect_add paramJsonPartial=["+paramJsonPartial+"] json=["+paramJsonSt+"]");
                
                String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                accumulateJson+=paramJsonSt;
                httpSession.setAttribute("accumulate", accumulateJson );
                actionAnswer.responseMap.put("status", "ok");

            }
            
			return actionAnswer;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			logger.severe("#### CraneTruck Exception ["+e.toString()+"] at "+exceptionDetails);
			actionAnswer.isResponseMap=true;
			actionAnswer.responseMap.put("Error", "CraneTruck Exception ["+e.toString()+"] at "+exceptionDetails);
			return actionAnswer;
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
