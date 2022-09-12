package org.goobi.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.goobi.beans.DatabaseObject;
import org.goobi.beans.Institution;
import org.goobi.beans.User;

import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.IManager;
import de.sub.goobi.persistence.managers.InstitutionManager;
import de.sub.goobi.persistence.managers.MySQLHelper;
import de.sub.goobi.persistence.managers.UserManager;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ExtendedUserManager implements IManager {

    private static final long serialVersionUID = -1229688637023945312L;

    @Override
    public int getHitSize(String order, String filter, Institution institution) {
        try {
            return getUserCount(filter);
        } catch (SQLException e) {
            log.error(e);
        }
        return 0;
    }

    @Override
    public List<Integer> getIdList(String order, String filter, Institution institution) {
        try {
            return getIdList(filter);
        } catch (SQLException e) {
            log.error(e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<? extends DatabaseObject> getList(String order, String filter, Integer start, Integer count, Institution institution) {
        try {
            return getUsers(order, filter, start, count);
        } catch (SQLException e) {
            log.error(e);
        }
        return Collections.emptyList();
    }

    public static void saveUser(ExtendedUser user) {
        try {
            UserManager.saveUser(user.getUser());
        } catch (DAOException e) {
            log.error(e);
        }
        InstitutionManager.saveInstitution(user.getInstitution());
    }

    public static void deleteUser(ExtendedUser user) {
        try {
            UserManager.deleteUser(user.getUser());
        } catch (DAOException e) {
            log.error(e);
        }
        InstitutionManager.deleteInstitution(user.getInstitution());

    }

    public static User getUserById(Integer id) {
        try {
            return UserManager.getUserById(id);
        } catch (DAOException e) {
            return null;
        }
    }

    private static List<ExtendedUser> getUsers(String order, String filter, Integer start, Integer count) throws SQLException {
        Connection connection = null;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT benutzer.*, institution.*, lastDate, items");
        createFromQuery(sql);
        if (filter != null && !filter.isEmpty()) {
            sql.append(" WHERE " + filter); //NOSONAR
        }
        if (order != null && !order.isEmpty()) {
            sql.append(" ORDER BY " + order);
        }
        if (start != null && count != null) {
            sql.append(" LIMIT " + start + ", " + count);
        }
        try {
            connection = MySQLHelper.getInstance().getConnection();
            return new QueryRunner().query(connection, sql.toString(), resultSetToUserListHandler);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    private static List<Integer> getIdList(String filter) throws SQLException {
        Connection connection = null;
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT benutzer.BenutzerID ");
        createFromQuery(sql);

        if (filter != null && !filter.isEmpty()) {
            sql.append(" WHERE " + filter);
        }
        try {
            connection = MySQLHelper.getInstance().getConnection();
            if (log.isTraceEnabled()) {
                log.trace(sql.toString());
            }
            return new QueryRunner().query(connection, sql.toString(), MySQLHelper.resultSetToIntegerListHandler);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    private static int getUserCount(String filter) throws SQLException {
        Connection connection = null;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT count(1) ");
        createFromQuery(sql);

        if (filter != null && !filter.isEmpty()) {
            sql.append(" WHERE " + filter);
        } //TODO
        try {
            connection = MySQLHelper.getInstance().getConnection();
            if (log.isTraceEnabled()) {
                log.trace(sql.toString());
            }
            return new QueryRunner().query(connection, sql.toString(), MySQLHelper.resultSetToIntegerHandler);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    private static ResultSetHandler<List<ExtendedUser>> resultSetToUserListHandler = new ResultSetHandler<List<ExtendedUser>>() {
        @Override
        public List<ExtendedUser> handle(ResultSet rs) throws SQLException {
            List<ExtendedUser> answer = new ArrayList<>();
            try {
                while (rs.next()) {
                    ExtendedUser o = convert(rs);
                    answer.add(o);
                }
            } finally {
                rs.close();
            }
            return answer;
        }
    };

    private static ExtendedUser convert(ResultSet rs) throws SQLException {
        User user = UserManager.convert(rs);

        Institution r = new Institution();
        r.setId(rs.getInt("institution.id"));
        r.setShortName(rs.getString("institution.shortName"));
        r.setLongName(rs.getString("institution.longName"));
        r.setAllowAllRulesets(rs.getBoolean("institution.allowAllRulesets"));
        r.setAllowAllDockets(rs.getBoolean("institution.allowAllDockets"));
        r.setAllowAllAuthentications(rs.getBoolean("institution.allowAllAuthentications"));
        r.setAllowAllPlugins(rs.getBoolean("institution.allowAllPlugins"));
        r.setAdditionalData(MySQLHelper.convertStringToMap(rs.getString("institution.additional_data")));

        user.setInstitution(r);

        ExtendedUser eu = new ExtendedUser(user);
        Timestamp time = rs.getTimestamp("lastDate");

        eu.setLastUploadDate(time == null ? null : new Date(time.getTime()));
        eu.setNumberOfUploads(rs.getInt("items"));
        return eu;
    }

    private static void createFromQuery(StringBuilder sb) {
        sb.append(" FROM ");
        sb.append("benutzer ");
        sb.append("LEFT JOIN ");
        sb.append("institution ON benutzer.institution_id = id ");
        sb.append("LEFT JOIN ");
        sb.append("(SELECT  ");
        sb.append("wert, ");
        sb.append("MAX(creationDate) AS lastDate, ");
        sb.append("COUNT(creationDate) AS items ");
        sb.append("FROM ");
        sb.append("prozesseeigenschaften ");
        sb.append("WHERE ");
        sb.append("titel = 'Institution' ");
        sb.append("GROUP BY wert) AS t ON institution.shortName = t.wert ");
    }

    public static boolean isInstitutionHasUserAssigned(Institution institution)  {
        if (institution.getId() == null) {
            return false;
        }
        String sql = "select count(1) from benutzer where institution_id = ?";
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            int numberOfUser = new QueryRunner().query(connection, sql, MySQLHelper.resultSetToIntegerHandler, institution.getId());
            return numberOfUser != 0;

        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (connection != null) {
                try {
                    MySQLHelper.closeConnection(connection);
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
        return false;
    }

}
