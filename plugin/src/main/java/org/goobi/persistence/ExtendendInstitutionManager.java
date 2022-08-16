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

import de.sub.goobi.persistence.managers.IManager;
import de.sub.goobi.persistence.managers.InstitutionManager;
import de.sub.goobi.persistence.managers.MySQLHelper;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ExtendendInstitutionManager implements IManager {

    @Override
    public int getHitSize(String order, String filter, Institution institution) {
        try {
            return getInstitutionCount(filter);
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
            return getInstitutions(order, filter, start, count);
        } catch (SQLException e) {
            log.error(e);
        }
        return Collections.emptyList();
    }

    public static void saveInstitution(ExtendedInstitution institution) {
        InstitutionManager.saveInstitution(institution.getInstitution());
    }

    public static void deleteInstitution(ExtendedInstitution institution) {
        InstitutionManager.deleteInstitution(institution.getInstitution());

    }

    public static Institution getInstitutionById(Integer id) {
        return InstitutionManager.getInstitutionById(id);
    }

    private static List<ExtendedInstitution> getInstitutions(String order, String filter, Integer start, Integer count) throws SQLException {
        Connection connection = null;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM institution LEFT JOIN (SELECT wert, MAX(creationDate) as lastDate, count(creationDate) as items ");
        sql.append("FROM prozesseeigenschaften WHERE titel = 'Institution' GROUP BY wert) AS t ON institution.shortName = t.wert");
        if (filter != null && !filter.isEmpty()) {
            sql.append(" WHERE " + filter);
        }
        if (order != null && !order.isEmpty()) {
            sql.append(" ORDER BY " + order);
        }
        if (start != null && count != null) {
            sql.append(" LIMIT " + start + ", " + count);
        }
        try {
            connection = MySQLHelper.getInstance().getConnection();
            List<ExtendedInstitution> ret = new QueryRunner().query(connection, sql.toString(), resultSetToInstitutionListHandler);
            return ret;
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    private static List<Integer> getIdList(String filter) throws SQLException {
        Connection connection = null;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id FROM institution");
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

    private static int getInstitutionCount(String filter) throws SQLException {
        Connection connection = null;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT count(1) FROM institution");
        if (filter != null && !filter.isEmpty()) {
            sql.append(" WHERE " + filter);
        }
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

    private static ResultSetHandler<List<ExtendedInstitution>> resultSetToInstitutionListHandler = new ResultSetHandler<List<ExtendedInstitution>>() {
        @Override
        public List<ExtendedInstitution> handle(ResultSet rs) throws SQLException {
            List<ExtendedInstitution> answer = new ArrayList<>();
            try {
                while (rs.next()) {
                    ExtendedInstitution o = convert(rs);
                    answer.add(o);
                }
            } finally {
                rs.close();
            }
            return answer;
        }
    };

    private static ExtendedInstitution convert(ResultSet rs) throws SQLException {
        Institution r = new Institution();
        r.setId(rs.getInt("id"));
        r.setShortName(rs.getString("shortName"));
        r.setLongName(rs.getString("longName"));
        r.setAllowAllRulesets(rs.getBoolean("allowAllRulesets"));
        r.setAllowAllDockets(rs.getBoolean("allowAllDockets"));
        r.setAllowAllAuthentications(rs.getBoolean("allowAllAuthentications"));
        r.setAllowAllPlugins(rs.getBoolean("allowAllPlugins"));
        r.setAdditionalData(MySQLHelper.convertStringToMap(rs.getString("additional_data")));

        ExtendedInstitution ei = new ExtendedInstitution(r);
        Timestamp time = rs.getTimestamp("lastDate");

        ei.setLastUploadDate(time == null ? null : new Date(time.getTime()));
        ei.setNumberOfUploads(rs.getInt("items"));
        return ei;
    }

}
