package org.goobi.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.goobi.beans.DatabaseObject;
import org.goobi.beans.Institution;

import de.sub.goobi.persistence.managers.IManager;
import de.sub.goobi.persistence.managers.InstitutionManager;
import de.sub.goobi.persistence.managers.MySQLHelper;
import lombok.extern.log4j.Log4j2;


@Log4j2
public class ExtendendInstitutionManager implements IManager{

    @Override
    public int getHitSize(String order, String filter, Institution institution) {
        try {
            return getInstitutionCount(order, filter);
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






    private  static List<Institution> getInstitutions(String order, String filter, Integer start, Integer count) throws SQLException {
        Connection connection = null;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM institution");
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
            List<Institution> ret = new QueryRunner().query(connection, sql.toString(), InstitutionManager.resultSetToInstitutionListHandler);
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

    private static int getInstitutionCount(String order, String filter) throws SQLException {
        Connection connection = null;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(1) FROM institution");
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

}
