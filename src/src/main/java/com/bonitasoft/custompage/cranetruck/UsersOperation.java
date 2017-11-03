package com.bonitasoft.custompage.cranetruck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserCriterion;
import org.bonitasoft.engine.identity.UserUpdater;
import org.bonitasoft.log.event.BEvent;
import org.json.simple.JSONValue;

import com.bonitasoft.custompage.cranetruck.Toolbox.StatusOperation;

public class UsersOperation {

    static Logger logger = Logger.getLogger(JaasCheck.class.getName());

    static final BEvent eventListError = new BEvent(UsersOperation.class.getName(), 1, BEvent.Level.ERROR,
            "Can't get the list of users", "The list of user is not accessible", "List is empty", "Check the Error (connection to the server died ? )");

    static final BEvent eventOperationOnUsersFailed = new BEvent(UsersOperation.class.getName(), 2, BEvent.Level.ERROR,
            "Update operation on user failed", "The requested operation failed", "Users (or part of users) does not run correctly", "Check the message");
    static final BEvent eventOperationOnUsersSuccess = new BEvent(UsersOperation.class.getName(), 3, BEvent.Level.SUCCESS,
            "Update operation on user success", "the requested operation is a success");

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Read parameters method */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    public int maxtodisplay;
    public String operation;
    public boolean simulateOperation = false;
    public String filteruser;
    public String scope;

    public static UsersOperation getInstanceFromJsonSt(final String jsonSt)
    {
        logger.info("Receive parametersJson=" + jsonSt);
        final UsersOperation jaasCheck = new UsersOperation();
        final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
        if (jsonHash == null) {
            logger.severe("Can't decode jsonSt " + jsonSt);

            return new UsersOperation();
        }
        jaasCheck.maxtodisplay = Toolbox.getInteger(jsonHash.get("maxtodisplay"), 100);
        jaasCheck.filteruser = Toolbox.getString(jsonHash.get("filteruser"), null);
        if (jaasCheck.filteruser != null && jaasCheck.filteruser.length() == 0) {
            jaasCheck.filteruser = null;
        }

        jaasCheck.operation = Toolbox.getString(jsonHash.get("operation"), null);
        jaasCheck.scope = Toolbox.getString(jsonHash.get("scope"), null);
        jaasCheck.simulateOperation = Toolbox.getBoolean(jsonHash.get("simulate"), true);
        return jaasCheck;
    }

    /**
     * @param identityApi
     * @return
     */
    public StatusOperation getUsersList(final IdentityAPI identityApi)
    {
        final StatusOperation statusOperation = new StatusOperation("UsersOperation");

        logger.info("GetUsersList V2 [" + maxtodisplay + "] filteruser[" + filteruser + "] ");
        try
        {
            statusOperation.mStatusResultJson.put("totalusers", identityApi.getNumberOfUsers());
            logger.info("getNumberOfUsers [" + identityApi.getNumberOfUsers() + "] ");

            final List<Map<String, Object>> listUsers = new ArrayList<Map<String, Object>>();
            int page = 0;
            do {
                final List<User> listPageUser = identityApi.getUsers(page, 100, UserCriterion.USER_NAME_ASC);
                if (listPageUser.size() == 0) {
                    logger.info(" no more user in page [" + page + "]");

                    break;
                }
                for (final User user : listPageUser)
                {
                    logger.info("userId[" + user.getId() + "] userName [" + user.getUserName() + "] keep? " + keepUser(user));
                    if (keepUser(user))
                    {
                        final HashMap<String, Object> oneUserMap = new HashMap<String, Object>();
                        oneUserMap.put("username", user.getUserName());
                        oneUserMap.put("firstname", user.getFirstName());
                        oneUserMap.put("lastname", user.getLastName());
                        oneUserMap.put("enable", user.isEnabled() ? "ACTIF" : "INACTIF");

                        listUsers.add(oneUserMap);
                    }
                    if (listUsers.size() >= maxtodisplay) {
                        break;
                    }
                }
                if (listUsers.size() >= maxtodisplay) {
                    break;
                }
                page += 100;
            } while (page < 10000);

            statusOperation.mStatusResultJson.put("list", listUsers);

        } catch (final Exception e)
        {
            logger.severe("GetUserList : " + e.toString());
            statusOperation.mStatusResultJson.put("error", e.toString());
            statusOperation.listEvents.add(new BEvent(eventListError, e, ""));
        }

        return statusOperation;
    }

    /**
     * @param identityApi
     * @param myself
     * @return
     */
    public StatusOperation doOperation(final IdentityAPI identityApi, final long myself) {

        final StatusOperation statusOperation = new StatusOperation("UsersOperation");

        try
        {
            logger.info("UserDoOperation [" + maxtodisplay + "] filteruser[" + filteruser + "] operation[" + operation + "] Scope[" + scope + "] simulate["
                    + simulateOperation + "]");

            String statusFinal = "";
            int numberOfError = 0;
            int numberOfOperations = 0;

            final List<User> listUsers = identityApi.getUsers(0, 100000, UserCriterion.USER_NAME_ASC);
            for (final User user : listUsers)
            {
                logger.info("doOperation user[" + user.getId() + "] userName[" + user.getUserName() + "] mySelf[" + myself + "] keepUser ? " + keepUser(user)
                        + "]");
                if (keepUser(user) && user.getId() != myself)
                {
                    if ("DISABLE".equals(operation))
                    {
                        final UserUpdater userUpdater = new UserUpdater();
                        userUpdater.setEnabled(false);
                        try
                        {
                            if (simulateOperation) {
                                logger.info("doOperation SIMULATE DISABLE user[" + user.getId() + "] userName[" + user.getUserName() + "]");
                            } else {
                                logger.info("doOperation DISABLE user[" + user.getId() + "] userName[" + user.getUserName() + "]");
                                identityApi.updateUser(user.getId(), userUpdater);
                            }
                            numberOfOperations++;
                        } catch (final Exception e)
                        {
                            logger.info("doOperation DISABLE user[" + user.getId() + "] userName[" + user.getUserName() + "] error " + e.toString());
                            statusFinal += "user[" + user.getFirstName() + "];";
                            numberOfError++;
                        }
                    }
                    else if ("ENABLE".equals(operation))
                    {
                        final UserUpdater userUpdater = new UserUpdater();
                        userUpdater.setEnabled(true);
                        try
                        {
                            if (simulateOperation) {
                                logger.info("doOperation SIMULATE ENABLE user[" + user.getId() + "] userName[" + user.getUserName() + "]");
                            } else {
                                identityApi.updateUser(user.getId(), userUpdater);
                            }
                            numberOfOperations++;
                        } catch (final Exception e)
                        {
                            logger.info("doOperation ENABLE user[" + user.getId() + "] userName[" + user.getUserName() + "] error " + e.toString());
                            statusFinal += "user[" + user.getFirstName() + "];";
                            numberOfError++;
                        }
                    }

                    else if ("DELETE".equals(operation))
                    {
                        try
                        {
                            if (simulateOperation) {
                                logger.info("doOperation SIMULATE DELETE user[" + user.getId() + "] userName[" + user.getUserName() + "]");
                            } else {
                                identityApi.deleteUser(user.getId());
                            }
                            numberOfOperations++;
                        } catch (final Exception e)
                        {
                            logger.info("doOperation DELETE user[" + user.getId() + "] userName[" + user.getUserName() + "] error " + e.toString());
                            statusFinal += "user[" + user.getFirstName() + "];";
                            numberOfError++;
                        }
                    }
                    else
                    {
                        statusFinal += "Unknown operation" + operation + "];";
                        numberOfError++;
                    }
                }
                if (numberOfError > 40) {
                    break;
                }
            }
            final String statusSuccess = "Number of operations done :" + numberOfOperations + " Number of errors:" + numberOfError;
            statusOperation.mStatusResultJson.put("status", statusSuccess);
            statusOperation.mStatusResultJson.put("statusfinal", statusFinal);
            if (numberOfError > 0) {
                statusOperation.mStatusResultJson.put("error", "Errors on " + statusFinal);
                statusOperation.listEvents.add(new BEvent(eventOperationOnUsersFailed, statusFinal));
            } else {
                statusOperation.listEvents.add(new BEvent(eventOperationOnUsersSuccess, statusSuccess));
            }
        } catch (final Exception e)
        {
            logger.severe("GetUserList : " + e.toString());
            statusOperation.mStatusResultJson.put("error", e.toString());
        }

        // play the get list in order to refresh the list

        final StatusOperation StatusOperationList = getUsersList(identityApi);
        statusOperation.mStatusResultJson.put("list", StatusOperationList.mStatusResultJson.get("list"));

        return statusOperation;

    }

    private boolean keepUser(final User user)
    {
        if (filteruser == null || user.getUserName().indexOf(filteruser) != -1) {
            return true;
        }
        return false;
    }
}
